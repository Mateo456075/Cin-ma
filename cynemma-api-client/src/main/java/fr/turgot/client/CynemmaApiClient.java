package fr.turgot.client;

import fr.turgot.client.service.HealthService;
import fr.turgot.client.service.TmdbService;
import fr.turgot.client.service.TrailerService;

/**
 * Point d'entrée principal du SDK Cynemma API Client.
 *
 * <p>Exemple d'instanciation :</p>
 * <pre>{@code
 * CynemmaApiClient client = CynemmaApiClient.builder()
 *         .baseUrl("http://localhost:8081/cynemma-api")
 *         .tokenProvider(() -> ApiJwtTokenService.issueEmployeeToken("alice"))
 *         .build();
 *
 * List<TmdbMovie> movies = client.tmdb().getNowPlaying();
 * }</pre>
 */
public interface CynemmaApiClient {

    /**
     * Accès au service de vérification de santé (/api/health).
     */
    HealthService health();

    /**
     * Accès au service des bandes-annonces (/api/trailer).
     */
    TrailerService trailers();

    /**
     * Accès au service catalogue TMDB (/api/internal/tmdb/*).
     */
    TmdbService tmdb();

    /**
     * Crée un nouveau builder pour configurer une instance de CynemmaApiClient.
     */
    static CynemmaApiClientBuilder builder() {
        return new CynemmaApiClientBuilder();
    }
}
