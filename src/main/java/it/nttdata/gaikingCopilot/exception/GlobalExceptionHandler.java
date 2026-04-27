package it.nttdata.gaikingCopilot.exception;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;



@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String CAUSE = "cause";

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(STATUS, ex.getStatusCode());
        body.put(ERROR, ex.getMessage());
        body.put(CAUSE, ex.getCause() != null ? ex.getCause().getMessage() : null);

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class, URISyntaxException.class, GitAPIException.class})
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(STATUS, 400);
        body.put(ERROR, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(STATUS, 500);
        body.put(ERROR, "Errore interno del server");
        body.put(CAUSE, ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
