package fr.turgot.client;

import fr.turgot.client.http.HttpRequestExecutor;
import fr.turgot.client.service.DefaultHealthService;
import fr.turgot.client.service.DefaultTmdbService;
import fr.turgot.client.service.DefaultTrailerService;
import fr.turgot.client.service.HealthService;
import fr.turgot.client.service.TmdbService;
import fr.turgot.client.service.TrailerService;

public class DefaultCynemmaApiClient implements CynemmaApiClient {
    private final HealthService healthService;
    private final TrailerService trailerService;
    private final TmdbService tmdbService;

    public DefaultCynemmaApiClient(HttpRequestExecutor executor) {
        this.healthService = new DefaultHealthService(executor);
        this.trailerService = new DefaultTrailerService(executor);
        this.tmdbService = new DefaultTmdbService(executor);
    }

    @Override
    public HealthService health() {
        return healthService;
    }

    @Override
    public TrailerService trailers() {
        return trailerService;
    }

    @Override
    public TmdbService tmdb() {
        return tmdbService;
    }
}
