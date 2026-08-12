package ec.edu.uteq.presustentaciones.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * AccessDeniedException es una RuntimeException, así que sin este handler específico
     * caía en el genérico de abajo y un rechazo de @PreAuthorize se reportaba como 400
     * en vez de 403 (hallazgo real detectado en OWASP-AUDIT.md).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensaje", "No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensaje", ex.getMessage()));
    }
}
