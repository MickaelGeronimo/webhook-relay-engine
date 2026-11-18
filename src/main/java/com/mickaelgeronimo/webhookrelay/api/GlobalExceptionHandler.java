package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.security.UnsafeWebhookTargetException;
import com.mickaelgeronimo.webhookrelay.service.InvalidDeliveryStateException;
import com.mickaelgeronimo.webhookrelay.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates domain, SSRF security and validation exceptions into RFC 7807 Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(UnsafeWebhookTargetException.class)
    public ProblemDetail handleUnsafeTarget(UnsafeWebhookTargetException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Unsafe webhook target URL");
        return problem;
    }

    @ExceptionHandler(InvalidDeliveryStateException.class)
    public ProblemDetail handleInvalidState(InvalidDeliveryStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid delivery state transition");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request payload failed validation");
        problem.setTitle("Validation error");
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> err.getDefaultMessage() == null ? "invalid" : err.getDefaultMessage(),
                        (first, second) -> first
                ));
        problem.setProperty("errors", fieldErrors);
        return problem;
    }
}
