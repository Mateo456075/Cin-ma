package fr.turgot.client;

import fr.turgot.client.auth.StaticTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CynemmaApiClientBuilderTest {

    @Test
    @DisplayName("Builder creates functional CynemmaApiClient instance")
    void testBuilderSuccess() {
        CynemmaApiClient client = CynemmaApiClient.builder()
                .baseUrl("http://localhost:8081/cynemma-api")
                .tokenProvider(new StaticTokenProvider("token-123"))
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        assertNotNull(client);
        assertNotNull(client.health());
        assertNotNull(client.trailers());
        assertNotNull(client.tmdb());
    }

    @Test
    @DisplayName("Builder with bearerToken string helper")
    void testBuilderTokenHelper() {
        CynemmaApiClient client = CynemmaApiClient.builder()
                .baseUrl("http://127.0.0.1:8080")
                .bearerToken("jwt-token-abc")
                .build();

        assertNotNull(client);
    }

    @Test
    @DisplayName("Builder validates baseUrl")
    void testBuilderBaseUrlValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                CynemmaApiClient.builder().baseUrl("").build());

        assertThrows(IllegalArgumentException.class, () ->
                CynemmaApiClient.builder().baseUrl(null).build());
    }
}
