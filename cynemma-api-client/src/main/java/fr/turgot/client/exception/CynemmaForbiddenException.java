package fr.turgot.client.exception;

/**
 * Levée lorsque l'API retourne HTTP 403 Forbidden (rôle requis insuffisant).
 */
public class CynemmaForbiddenException extends CynemmaApiException {
    public CynemmaForbiddenException(String message, String errorBody) {
        super(403, message, errorBody);
    }
}
