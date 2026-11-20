package fr.turgot.client.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;
import fr.turgot.client.dto.TrailerResponse;
import fr.turgot.client.http.HttpRequestExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTrailerServiceTest {

    private static HttpServer server;
    private static TrailerService trailerService;

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/cynemma-api/api/trailer", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("film=Inception")) {
                byte[] body = "{\"key\":\"dQw4w9WgXcQ\",\"watchUrl\":\"https://youtube.com/watch?v=dQw4w9WgXcQ\",\"embedUrl\":\"https://youtube.com/embed/dQw4w9WgXcQ\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                byte[] body = "{\"error\":\"Trailer not found\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });

        server.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequestExecutor executor = new HttpRequestExecutor(
                "http://127.0.0.1:" + port + "/cynemma-api",
                client,
                Duration.ofSeconds(5),
                null
        );
        trailerService = new DefaultTrailerService(executor);
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("getTrailer returns trailer when found")
    void getTrailerReturnsTrailer() {
        Optional<TrailerResponse> result = trailerService.getTrailer("Inception", 2010);
        assertTrue(result.isPresent());
        assertEquals("dQw4w9WgXcQ", result.get().key());
        assertEquals("https://youtube.com/watch?v=dQw4w9WgXcQ", result.get().watchUrl());
    }

    @Test
    @DisplayName("getTrailer returns empty Optional on 404")
    void getTrailerReturnsEmptyOn404() {
        Optional<TrailerResponse> result = trailerService.getTrailer("UnknownMovie", null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTrailer rejects null or blank film title")
    void getTrailerRejectsInvalidTitle() {
        assertThrows(IllegalArgumentException.class, () -> trailerService.getTrailer(null, null));
        assertThrows(IllegalArgumentException.class, () -> trailerService.getTrailer("   ", 2020));
    }
}
