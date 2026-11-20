package fr.turgot.client.auth;

/**
 * Fournisseur de jeton d'authentification Bearer (JWT) pour les requêtes vers l'API.
 */
@FunctionalInterface
public interface TokenProvider {
    /**
     * Retourne le jeton d'accès courant ou en génère un nouveau si nécessaire.
     *
     * @return le jeton JWT, ou {@code null} si aucun jeton n'est disponible.
     */
    String getToken();
}
