package fr.turgot.client.service;

import fr.turgot.client.dto.HealthResponse;

/**
 * Service de vérification de disponibilité de l'API Cynemma.
 */
public interface HealthService {
    /**
     * Interroge l'endpoint GET /api/health pour vérifier l'état du microservice et de la base de données.
     *
     * @return l'état de santé du service.
     */
    HealthResponse checkHealth();
}
