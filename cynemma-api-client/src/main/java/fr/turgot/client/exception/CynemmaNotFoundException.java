package fr.turgot.client.exception;

/**
 * Levée lorsque l'API retourne HTTP 404 Not Found.
 */
public class CynemmaNotFoundException extends CynemmaApiException {
    public CynemmaNotFoundException(String message, String errorBody) {
        super(404, message, errorBody);
    }
}
