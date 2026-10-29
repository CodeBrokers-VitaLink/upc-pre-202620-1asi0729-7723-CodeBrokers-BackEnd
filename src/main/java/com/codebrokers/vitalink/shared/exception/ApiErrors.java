package com.codebrokers.vitalink.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Component
public class ApiErrors {
    private final MessageSource messages;
    private final ObjectMapper mapper;

    public ApiErrors(MessageSource messages, ObjectMapper mapper) { this.messages = messages; this.mapper = mapper; }

    public ApiError body(String code, HttpServletRequest request, List<ApiError.FieldError> fields) {
        Locale locale = request.getLocale().getLanguage().equals("es") ? Locale.forLanguageTag("es") : Locale.ENGLISH;
        return new ApiError(status(code), code, messages.getMessage(code, null, locale), request.getRequestURI(), Instant.now(), fields);
    }

    public int status(String code) {
        return switch (code) {
            case "INVALID_CREDENTIALS", "UNAUTHENTICATED" -> 401;
            case "ACCESS_DENIED", "PATIENT_ACCESS_DENIED" -> 403;
            case "EMAIL_ALREADY_EXISTS" -> 409;
            case "AFFILIATIONS_UNAVAILABLE", "SERVICE_UNAVAILABLE" -> 503;
            case "NOT_FOUND" -> 404;
            case "METHOD_NOT_ALLOWED" -> 405;
            default -> 400;
        };
    }

    public void write(String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(status(code));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (status(code) == 401) response.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(response.getOutputStream(), body(code, request, List.of()));
    }
}
