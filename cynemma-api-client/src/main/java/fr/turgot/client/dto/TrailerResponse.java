package fr.turgot.client.dto;

/**
 * Réponse retournée par l'endpoint /api/trailer de l'API Cynemma.
 */
public record TrailerResponse(
        String key,
        String watchUrl,
        String embedUrl) {

    public String getKey() {
        return key;
    }

    public String getWatchUrl() {
        return watchUrl;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }
}
