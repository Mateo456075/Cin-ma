package fr.turgot.client.dto;

/**
 * Réponse retournée par l'endpoint de vérification de santé /api/health.
 */
public record HealthResponse(
        String status,
        String database) {

    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status)
                && ("CONNECTED".equalsIgnoreCase(database) || "UP".equalsIgnoreCase(database));
    }

    public String getStatus() {
        return status;
    }

    public String getDatabase() {
        return database;
    }
}
