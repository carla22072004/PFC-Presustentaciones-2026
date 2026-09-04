package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * MethodArgumentNotValidException (fallo de @Valid en el body, p. ej. email vacío o
     * password menor a 6 caracteres) no es una RuntimeException, así que sin este handler
     * caía en el genérico de Exception y se reportaba como 500 en vez de 400 -- hallazgo real
     * detectado al preparar los casos de "validación" de docs/postman/PFC-Collection.json
     * (Fase 10).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error("Error de validación en los datos enviados", errores));
    }

    /**
     * AccessDeniedException es una RuntimeException, así que sin este handler específico
     * caía en el genérico de abajo y un rechazo de @PreAuthorize se reportaba como 400
     * en vez de 403 (hallazgo real detectado en OWASP-AUDIT.md).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ResponseWrapper.error("No tienes permisos para realizar esta acción"));
    }

    /**
     * BadCredentialsException (login fallido) es una AuthenticationException, que a su vez
     * es una RuntimeException: sin este handler caía en el genérico y el frontend recibía
     * 400 en vez de 401, mostrando "Error de Conexión" en lugar de "credenciales incorrectas".
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ResponseWrapper.error("Correo o contraseña incorrectos"));
    }

    /** NoResourceFoundException (Spring 6): ruta no encontrada → 404, no 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error("Recurso no encontrado: " + ex.getResourcePath()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error(ex.getMessage()));
    }

    /**
     * IllegalStateException representa un conflicto con el estado actual del recurso
     * (p. ej. "el tema ya está guardado", "la fase ya fue aprobada"): 409, no 400 ni
     * el 500 genérico. Conserva el mensaje del servicio para que el frontend lo muestre.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ResponseWrapper.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleRuntimeException(RuntimeException ex) {
        // Log the exception for internal debugging (to be added to proper logger later if needed)
        System.err.println("RuntimeException: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseWrapper.error("Solicitud inválida o error en el proceso"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<Object>> handleGeneralException(Exception ex) {
        // Log the exception for internal debugging
        System.err.println("Exception: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Ha ocurrido un error interno en el servidor. Por favor, inténtelo de nuevo más tarde."));
    }
}
