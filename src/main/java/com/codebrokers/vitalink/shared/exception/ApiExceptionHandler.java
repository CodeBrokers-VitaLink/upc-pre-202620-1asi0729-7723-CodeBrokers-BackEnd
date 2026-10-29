package com.codebrokers.vitalink.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final ApiErrors errors;
    public ApiExceptionHandler(ApiErrors errors) { this.errors = errors; }

    @ExceptionHandler(ApplicationFailure.class)
    ResponseEntity<ApiError> application(ApplicationFailure ex, HttpServletRequest request) {
        return response(ex.code(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream().map(field ->
                new ApiError.FieldError(field.getField(), "INVALID_VALUE")).distinct().toList();
        return response("VALIDATION_ERROR", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest request) { return response("VALIDATION_ERROR", request, List.of()); }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> unavailable(HttpServletRequest request) { return response("SERVICE_UNAVAILABLE", request, List.of()); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(HttpServletRequest request) { return response("ACCESS_DENIED", request, List.of()); }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(HttpServletRequest request) { return response("NOT_FOUND", request, List.of()); }

    private ResponseEntity<ApiError> response(String code, HttpServletRequest request, List<ApiError.FieldError> fields) {
        return ResponseEntity.status(errors.status(code)).header("Cache-Control", "no-store").body(errors.body(code, request, fields));
    }
}
