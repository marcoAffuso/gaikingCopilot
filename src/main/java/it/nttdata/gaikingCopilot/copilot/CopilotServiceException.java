package it.nttdata.gaikingCopilot.copilot;

public class CopilotServiceException extends RuntimeException {

    private final int statusCode;

    public CopilotServiceException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
