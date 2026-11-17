package com.upc.numerix.exceptions;

import com.upc.numerix.dtos.ErrorDTO;
import com.upc.numerix.util.Constantes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

// Convierte las excepciones en respuestas JSON con un mensaje que el frontend puede mostrar
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OperacionRechazadaException.class)
    public ResponseEntity<ErrorDTO> operacionRechazada(OperacionRechazadaException ex) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, Constantes.RECHAZADA, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDTO> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, null, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String mensaje = errores.values().stream().findFirst().orElse("Datos no válidos.");
        return respuesta(HttpStatus.BAD_REQUEST, Constantes.RECHAZADA, mensaje, errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorDTO> formato(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, Constantes.RECHAZADA,
                "Formato de datos no válido. Las fechas usan el formato YYYY-MM-DD.", null);
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ErrorDTO> credenciales(Exception ex) {
        String mensaje = ex instanceof DisabledException
                ? "La cuenta se encuentra inactiva."
                : "Usuario o contraseña incorrectos.";
        return respuesta(HttpStatus.UNAUTHORIZED, null, mensaje, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDTO> accesoDenegado(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, null, "No tiene permiso para realizar esta operación.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> integridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT, Constantes.RECHAZADA,
                "El registro entra en conflicto con datos existentes (valor duplicado).", null);
    }

    private ResponseEntity<ErrorDTO> respuesta(HttpStatus estado, String estadoOperacion, String mensaje,
                                               Map<String, String> errores) {
        return new ResponseEntity<>(new ErrorDTO(estadoOperacion, mensaje, errores), estado);
    }
}
