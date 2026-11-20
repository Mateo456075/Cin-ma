package fr.turgot.client.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;
import fr.turgot.client.dto.HealthResponse;
import fr.turgot.client.http.HttpRequestExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultHealthServiceTest {

    private static HttpServer server;
    private static HealthService healthService;
    private static HealthService brokenHealthService;

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/cynemma-api/api/health", exchange -> {
            byte[] body = "{\"status\":\"UP\",\"database\":\"CONNECTED\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
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
        healthService = new DefaultHealthService(executor);

        HttpRequestExecutor deadExecutor = new HttpRequestExecutor(
                "http://127.0.0.1:1/cynemma-api",
                client,
                Duration.ofMillis(200),
                null
        );
        brokenHealthService = new DefaultHealthService(deadExecutor);
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("checkHealth returns parsed HealthResponse")
    void checkHealthReturnsParsedResponse() {
        HealthResponse response = healthService.checkHealth();
        assertNotNull(response);
        assertEquals("UP", response.status());
        assertEquals("CONNECTED", response.database());
        assertTrue(response.isHealthy());
    }

    @Test
    @DisplayName("checkHealth throws on network failure")
    void checkHealthThrowsOnNetworkFailure() {
        assertThrows(RuntimeException.class, () -> brokenHealthService.checkHealth());
    }
}
