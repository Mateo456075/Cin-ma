package fr.turgot.client.dto;

/**
 * Requête envoyée pour mettre en cache la bande-annonce d'un film.
 */
public record TrailerCacheRequest(String title) {
    public TrailerCacheRequest {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre du film ne peut pas être vide.");
        }
    }
}
