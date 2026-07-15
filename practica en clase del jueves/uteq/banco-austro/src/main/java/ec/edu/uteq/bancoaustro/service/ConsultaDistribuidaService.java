package ec.edu.uteq.bancoaustro.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ConsultaDistribuidaService {
    private static final String SQL_CLIENTES = "SELECT cedula, nombre, ciudad FROM clientes";

    private final Nodo cuenca;
    private final Nodo quito;
    private final Nodo guayaquil;

    public ConsultaDistribuidaService(@Qualifier("dsCuenca") DataSource dsCuenca,
                                      @Qualifier("dsQuito") DataSource dsQuito,
                                      @Qualifier("dsGuayaquil") DataSource dsGuayaquil) {
        this.cuenca = crearNodo("CUENCA", dsCuenca);
        this.quito = crearNodo("QUITO", dsQuito);
        this.guayaquil = crearNodo("GUAYAQUIL", dsGuayaquil);
    }

    public Map<String, Object> consultarSaldo(String numero) {
        String sql = "SELECT numero, saldo, oficina FROM cuentas WHERE numero = ?";
        List<Map<String, Object>> filas = enrutar(numero).jdbc().queryForList(sql, numero);
        return filas.isEmpty() ? Map.of("error", "Cuenta no encontrada", "numero", numero) : filas.getFirst();
    }

    public Map<String, Object> listarTodosLosClientes() {
        List<Map<String, Object>> clientes = new ArrayList<>();
        List<String> nodosNoDisponibles = new ArrayList<>();

        agregarClientesDisponibles(cuenca, clientes, nodosNoDisponibles);
        agregarClientesDisponibles(quito, clientes, nodosNoDisponibles);
        agregarClientesDisponibles(guayaquil, clientes, nodosNoDisponibles);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("clientes", clientes);
        respuesta.put("total", clientes.size());
        respuesta.put("nodosNoDisponibles", nodosNoDisponibles);
        respuesta.put("advertencia", nodosNoDisponibles.isEmpty()
                ? null
                : "Respuesta parcial. No fue posible consultar: " + String.join(", ", nodosNoDisponibles));
        return respuesta;
    }

    public Map<String, Object> transferir(String origen, String destino, BigDecimal monto) {
        validarTransferencia(origen, destino, monto);
        Nodo nodoOrigen = enrutar(origen);
        Nodo nodoDestino = enrutar(destino);

        if (nodoOrigen == nodoDestino) {
            nodoOrigen.transaccion().executeWithoutResult(status -> {
                debitar(nodoOrigen.jdbc(), origen, monto);
                acreditar(nodoDestino.jdbc(), destino, monto);
                registrar(nodoOrigen, origen, destino, monto);
            });
        } else {
            transferirEntreSedes(nodoOrigen, nodoDestino, origen, destino, monto);
        }

        return respuestaTransferencia(nodoOrigen, nodoDestino, origen, destino, monto);
    }

    private void transferirEntreSedes(Nodo nodoOrigen, Nodo nodoDestino,
                                      String origen, String destino, BigDecimal monto) {
        verificarCuenta(nodoOrigen.jdbc(), origen);
        verificarCuenta(nodoDestino.jdbc(), destino);

        nodoOrigen.transaccion().executeWithoutResult(status -> debitar(nodoOrigen.jdbc(), origen, monto));
        try {
            nodoDestino.transaccion().executeWithoutResult(status -> acreditar(nodoDestino.jdbc(), destino, monto));
        } catch (RuntimeException errorDestino) {
            try {
                nodoOrigen.transaccion().executeWithoutResult(status -> acreditar(nodoOrigen.jdbc(), origen, monto));
            } catch (RuntimeException errorCompensacion) {
                errorDestino.addSuppressed(errorCompensacion);
                throw new IllegalStateException("Fallo la transferencia y no se pudo compensar el debito", errorDestino);
            }
            throw new IllegalStateException("Transferencia cancelada; el debito fue compensado", errorDestino);
        }
        registrar(nodoOrigen, origen, destino, monto);
    }

    private Map<String, Object> respuestaTransferencia(Nodo nodoOrigen, Nodo nodoDestino,
                                                       String origen, String destino, BigDecimal monto) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "COMPLETADA");
        respuesta.put("tipo", nodoOrigen == nodoDestino ? "LOCAL" : "ENTRE_SEDES");
        respuesta.put("origen", origen);
        respuesta.put("destino", destino);
        respuesta.put("monto", monto);
        respuesta.put("oficinaOrigen", nodoOrigen.oficina());
        respuesta.put("oficinaDestino", nodoDestino.oficina());
        respuesta.put("saldoOrigen", saldo(nodoOrigen.jdbc(), origen));
        respuesta.put("saldoDestino", saldo(nodoDestino.jdbc(), destino));
        return respuesta;
    }

    private void agregarClientesDisponibles(Nodo nodo, List<Map<String, Object>> clientes,
                                             List<String> nodosNoDisponibles) {
        try {
            clientes.addAll(nodo.jdbc().queryForList(SQL_CLIENTES));
        } catch (DataAccessException error) {
            nodosNoDisponibles.add(nodo.oficina());
        }
    }

    private void validarTransferencia(String origen, String destino, BigDecimal monto) {
        if (origen == null || destino == null || origen.equals(destino)) {
            throw new IllegalArgumentException("Las cuentas de origen y destino deben ser diferentes");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        enrutar(origen);
        enrutar(destino);
    }

    private void debitar(JdbcTemplate jdbc, String numero, BigDecimal monto) {
        int actualizadas = jdbc.update(
                "UPDATE cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?",
                monto, numero, monto);
        if (actualizadas == 0) {
            verificarCuenta(jdbc, numero);
            throw new IllegalArgumentException("Saldo insuficiente en la cuenta " + numero);
        }
    }

    private void acreditar(JdbcTemplate jdbc, String numero, BigDecimal monto) {
        int actualizadas = jdbc.update("UPDATE cuentas SET saldo = saldo + ? WHERE numero = ?", monto, numero);
        if (actualizadas == 0) {
            throw new IllegalArgumentException("Cuenta no encontrada: " + numero);
        }
    }

    private void verificarCuenta(JdbcTemplate jdbc, String numero) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cuentas WHERE numero = ?", Integer.class, numero);
        if (cantidad == null || cantidad == 0) {
            throw new IllegalArgumentException("Cuenta no encontrada: " + numero);
        }
    }

    private BigDecimal saldo(JdbcTemplate jdbc, String numero) {
        return jdbc.queryForObject("SELECT saldo FROM cuentas WHERE numero = ?", BigDecimal.class, numero);
    }

    private void registrar(Nodo nodo, String origen, String destino, BigDecimal monto) {
        nodo.jdbc().update(
                "INSERT INTO transacciones (cuenta_orig, cuenta_dest, monto, oficina) VALUES (?, ?, ?, ?)",
                origen, destino, monto, nodo.oficina());
    }

    private Nodo enrutar(String numero) {
        if (numero == null || numero.length() < 2) {
            throw new IllegalArgumentException("Numero de cuenta invalido: " + numero);
        }
        return switch (numero.substring(0, 2)) {
            case "22" -> cuenca;
            case "17" -> quito;
            case "09" -> guayaquil;
            default -> throw new IllegalArgumentException(
                    "Prefijo de oficina no reconocido: " + numero.substring(0, 2));
        };
    }

    private Nodo crearNodo(String oficina, DataSource dataSource) {
        return new Nodo(
                oficina,
                new JdbcTemplate(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    private record Nodo(String oficina, JdbcTemplate jdbc, TransactionTemplate transaccion) {
    }
}
