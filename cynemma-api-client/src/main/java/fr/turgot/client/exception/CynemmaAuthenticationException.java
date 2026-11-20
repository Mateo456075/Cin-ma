package fr.turgot.client.exception;

/**
 * Levée lorsque l'API retourne HTTP 401 Unauthorized (jeton JWT absent, expiré ou invalide).
 */
public class CynemmaAuthenticationException extends CynemmaApiException {
    public CynemmaAuthenticationException(String message, String errorBody) {
        super(401, message, errorBody);
    }
}
