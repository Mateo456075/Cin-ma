package fr.turgot.client.exception;

/**
 * Levée lorsque l'API retourne HTTP 400 Bad Request ou HTTP 422 Unprocessable Entity.
 */
public class CynemmaValidationException extends CynemmaApiException {
    public CynemmaValidationException(int statusCode, String message, String errorBody) {
        super(statusCode, message, errorBody);
    }
}
