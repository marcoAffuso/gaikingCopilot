package it.nttdata.gaikingCopilot.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final int statusCode;
    private final Throwable cause;
    private final String message;

    public CustomException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.cause = cause;
        this.message = message;
    }    

}
