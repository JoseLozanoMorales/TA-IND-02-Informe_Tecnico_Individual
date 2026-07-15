package ec.edu.uteq.bancoaustro.controller;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> solicitudInvalida(IllegalArgumentException error) {
        return respuesta(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, Object>> nodoNoDisponible(DataAccessException error) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "El nodo solicitado no esta disponible");
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> transferenciaFallida(IllegalStateException error) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, error.getMessage());
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(
                "estado", estado.value(),
                "error", mensaje));
    }
}
