package fr.auclairdeso.shared.web;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every API error as a ProblemDetail, and lists the invalid fields of a rejected request body.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ex.getBody();
        problem.setDetail("Certaines informations sont invalides.");
        problem.setProperty("errors", ex.getBindingResult().getAllErrors().stream()
            .map(ApiExceptionHandler::toFieldMessage)
            .toList());
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static FieldMessage toFieldMessage(ObjectError error) {
        var field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return new FieldMessage(field, error.getDefaultMessage());
    }

    record FieldMessage(String field, @Nullable String message) {
    }
}
