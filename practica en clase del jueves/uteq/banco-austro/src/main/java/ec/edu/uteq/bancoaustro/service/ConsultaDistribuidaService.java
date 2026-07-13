package ec.edu.uteq.bancoaustro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConsultaDistribuidaService {
    private final JdbcTemplate jdbcCuenca;
    private final JdbcTemplate jdbcQuito;

    public ConsultaDistribuidaService(@Qualifier("dsCuenca") DataSource dsCuenca,
                                      @Qualifier("dsQuito") DataSource dsQuito) {
        this.jdbcCuenca = new JdbcTemplate(dsCuenca);
        this.jdbcQuito = new JdbcTemplate(dsQuito);
    }

    public Map<String, Object> consultarSaldo(String numero) {
        String sql = "SELECT numero, saldo, oficina FROM cuentas WHERE numero = ?";
        List<Map<String, Object>> filas = enrutar(numero).queryForList(sql, numero);
        return filas.isEmpty() ? Map.of("error", "Cuenta no encontrada", "numero", numero) : filas.getFirst();
    }

    public List<Map<String, Object>> listarTodosLosClientes() {
        String sql = "SELECT cedula, nombre, ciudad FROM clientes";
        List<Map<String, Object>> union = new ArrayList<>();
        union.addAll(jdbcCuenca.queryForList(sql));
        union.addAll(jdbcQuito.queryForList(sql));
        return union;
    }

    private JdbcTemplate enrutar(String numero) {
        if (numero == null || numero.length() < 2) {
            throw new IllegalArgumentException("Numero de cuenta invalido: " + numero);
        }
        return switch (numero.substring(0, 2)) {
            case "22" -> jdbcCuenca;
            case "17" -> jdbcQuito;
            default -> throw new IllegalArgumentException("Prefijo de oficina no reconocido: " + numero.substring(0, 2));
        };
    }
}
