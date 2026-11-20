package com.banco.controlador;

import com.banco.modelo.Errores;
import com.banco.vista.ApiDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * CONTROLADOR - Traduce las excepciones del modelo a la vista ApiError.
 */
@RestControllerAdvice
@Slf4j
public class ApiErrors {

    @ExceptionHandler(Errores.Timeout.class)
    public ResponseEntity<ApiDtos.ApiError> timeout(Errores.Timeout e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiDtos.ApiError.de("TIMEOUT", e.getMessage()));
    }

    @ExceptionHandler(Errores.NoDisponible.class)
    public ResponseEntity<ApiDtos.ApiError> noDisponible(Errores.NoDisponible e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiDtos.ApiError.de("DLL NO DISPONIBLE", e.getMessage()));
    }

    @ExceptionHandler(Errores.Cobol.class)
    public ResponseEntity<ApiDtos.ApiError> cobol(Errores.Cobol e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiDtos.ApiError.de("ERROR RUNTIME COBOL", e.getMessage()));
    }

    @ExceptionHandler(Errores.Negocio.class)
    public ResponseEntity<ApiDtos.ApiError> negocio(Errores.Negocio e) {
        return ResponseEntity.badRequest().body(ApiDtos.ApiError.de(e.getMessage()));
    }

    @ExceptionHandler(Errores.NoEncontrado.class)
    public ResponseEntity<ApiDtos.ApiError> noEncontrado(Errores.NoEncontrado e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiDtos.ApiError.de(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiDtos.ApiError> argumento(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiDtos.ApiError.de(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiDtos.ApiError> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(ApiDtos.ApiError.de("VALIDACION", detalle));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiDtos.ApiError> inesperado(Exception e) {
        log.error("Error interno no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiDtos.ApiError.de("ERROR INTERNO",
                        e.getMessage() != null ? e.getMessage() : e.toString()));
    }
}
