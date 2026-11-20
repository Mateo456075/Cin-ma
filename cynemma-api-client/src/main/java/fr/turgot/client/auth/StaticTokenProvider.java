package fr.turgot.client.auth;

/**
 * Implémentation simple d'un TokenProvider fournissant un jeton fixe.
 */
public record StaticTokenProvider(String token) implements TokenProvider {
    @Override
    public String getToken() {
        return token;
    }
}
