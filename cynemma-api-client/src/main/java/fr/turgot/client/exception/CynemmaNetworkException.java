package fr.turgot.client.exception;

/**
 * Levée en cas d'erreur réseau, dépassement de délai (timeout) ou interruption de communication.
 */
public class CynemmaNetworkException extends CynemmaApiException {
    public CynemmaNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
