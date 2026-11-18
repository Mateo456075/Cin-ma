package fr.turgot.service;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

public class TmdbApiClientTest {
    @Test
    public void allowsHttpsForRemoteApi() {
        URI uri = TmdbApiClient.buildApiUri("https://api.example.test/cynemma-api", "/api/health");

        assertEquals("https", uri.getScheme());
        assertEquals("api.example.test", uri.getHost());
    }

    @Test
    public void allowsHttpForLocalDevelopment() {
        URI uri = TmdbApiClient.buildApiUri("http://localhost:8081/cynemma-api", "/api/health");

        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsHttpForRemoteApi() {
        TmdbApiClient.buildApiUri("http://api.example.test/cynemma-api", "/api/internal/tmdb");
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsHttpWhenUrlContainsCredentials() {
        TmdbApiClient.buildApiUri("http://user:password@localhost:8081/cynemma-api", "/api/health");
    }
}
