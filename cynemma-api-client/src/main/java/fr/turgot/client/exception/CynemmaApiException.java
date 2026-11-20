package fr.turgot.client.exception;

/**
 * Exception racine levée par le client d'API Cynemma.
 */
public class CynemmaApiException extends RuntimeException {
    private final int statusCode;
    private final String errorBody;

    public CynemmaApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.errorBody = null;
    }

    public CynemmaApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorBody = null;
    }

    public CynemmaApiException(int statusCode, String message, String errorBody) {
        super(message);
        this.statusCode = statusCode;
        this.errorBody = errorBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorBody() {
        return errorBody;
    }
}
