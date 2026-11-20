package fr.turgot.client.exception;

/**
 * Levée lorsque l'API retourne HTTP 502 Bad Gateway, 503 Service Unavailable ou 504 Gateway Timeout.
 */
public class CynemmaServiceUnavailableException extends CynemmaApiException {
    public CynemmaServiceUnavailableException(int statusCode, String message, String errorBody) {
        super(statusCode, message, errorBody);
    }
}
