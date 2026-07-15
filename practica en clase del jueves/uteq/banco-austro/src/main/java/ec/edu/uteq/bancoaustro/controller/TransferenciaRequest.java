package ec.edu.uteq.bancoaustro.controller;

import java.math.BigDecimal;

public record TransferenciaRequest(String origen, String destino, BigDecimal monto) {
}
