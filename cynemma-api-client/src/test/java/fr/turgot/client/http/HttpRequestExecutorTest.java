package fr.turgot.client.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.turgot.client.auth.StaticTokenProvider;
import fr.turgot.client.exception.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestExecutorTest {

    private static HttpServer server;
    private static int port;
    private static HttpRequestExecutor executor;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        // Endpoints
        server.createContext("/cynemma-api/api/health", exchange -> {
            byte[] body = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/internal/tmdb/now-playing", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (!"Bearer test-token".equals(auth)) {
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/error/400", exchange -> {
            byte[] body = "{\"error\":\"Bad request\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/error/401", exchange -> {
            byte[] body = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/error/403", exchange -> {
            byte[] body = "{\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/error/404", exchange -> {
            byte[] body = "{\"error\":\"Not found\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/error/503", exchange -> {
            byte[] body = "{\"error\":\"Unavailable\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.createContext("/cynemma-api/api/poster", exchange -> {
            byte[] sample = new byte[]{0x12, 0x34, 0x56};
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, sample.length);
            exchange.getResponseBody().write(sample);
            exchange.close();
        });

        server.start();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        executor = new HttpRequestExecutor(
                "http://127.0.0.1:" + port + "/cynemma-api",
                client,
                Duration.ofSeconds(5),
                new StaticTokenProvider("test-token")
        );
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Should reject invalid or insecure non-loopback HTTP URLs")
    void shouldRejectInsecureExternalHttpUrls() {
        HttpClient client = HttpClient.newHttpClient();
        assertThrows(IllegalArgumentException.class, () ->
                new HttpRequestExecutor("http://example.com/api", client, Duration.ofSeconds(5), null));

        assertThrows(IllegalArgumentException.class, () ->
                new HttpRequestExecutor("ftp://localhost:8081", client, Duration.ofSeconds(5), null));

        assertThrows(IllegalArgumentException.class, () ->
                new HttpRequestExecutor("http://192.168.1.10:8081", client, Duration.ofSeconds(5), null));
    }

    @Test
    @DisplayName("Should accept valid HTTPS and localhost loopback URLs")
    void shouldAcceptValidUrls() {
        HttpClient client = HttpClient.newHttpClient();
        assertDoesNotThrow(() ->
                new HttpRequestExecutor("https://api.cynemma.fr", client, Duration.ofSeconds(5), null));
        assertDoesNotThrow(() ->
                new HttpRequestExecutor("http://127.0.0.1:8081", client, Duration.ofSeconds(5), null));
        assertDoesNotThrow(() ->
                new HttpRequestExecutor("http://localhost:8081", client, Duration.ofSeconds(5), null));
    }

    @Test
    @DisplayName("Successful GET string response with status 200")
    void successfulGetString() {
        var response = executor.executeStringRequest("/api/health", "GET", null, null, null);
        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"UP\"}", response.body());
    }

    @Test
    @DisplayName("GET with authenticated request attaches Bearer token")
    void authenticatedGetAttachesBearer() {
        var response = executor.executeStringRequest("/api/internal/tmdb/now-playing", "GET", null, null, null);
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    @DisplayName("GET without token when auth is required throws CynemmaAuthenticationException")
    void getAuthRequiredWithoutTokenThrowsAuthException() {
        HttpRequestExecutor noAuthExecutor = new HttpRequestExecutor(
                "http://127.0.0.1:" + port + "/cynemma-api",
                HttpClient.newHttpClient(),
                Duration.ofSeconds(5),
                null
        );

        assertThrows(CynemmaAuthenticationException.class, () ->
                noAuthExecutor.executeStringRequest("/api/internal/tmdb/now-playing", "GET", null, null, null));
    }

    @Test
    @DisplayName("Status 400 maps to CynemmaValidationException")
    void status400MapsToValidationException() {
        CynemmaValidationException ex = assertThrows(CynemmaValidationException.class, () ->
                executor.executeStringRequest("/api/error/400", "GET", null, null, null));
        assertEquals(400, ex.getStatusCode());
        assertEquals("{\"error\":\"Bad request\"}", ex.getErrorBody());
    }

    @Test
    @DisplayName("Status 401 maps to CynemmaAuthenticationException")
    void status401MapsToAuthenticationException() {
        assertThrows(CynemmaAuthenticationException.class, () ->
                executor.executeStringRequest("/api/error/401", "GET", null, null, null));
    }

    @Test
    @DisplayName("Status 403 maps to CynemmaForbiddenException")
    void status403MapsToForbiddenException() {
        assertThrows(CynemmaForbiddenException.class, () ->
                executor.executeStringRequest("/api/error/403", "GET", null, null, null));
    }

    @Test
    @DisplayName("Status 404 maps to CynemmaNotFoundException")
    void status404MapsToNotFoundException() {
        assertThrows(CynemmaNotFoundException.class, () ->
                executor.executeStringRequest("/api/error/404", "GET", null, null, null));
    }

    @Test
    @DisplayName("Status 503 maps to CynemmaServiceUnavailableException")
    void status503MapsToServiceUnavailableException() {
        assertThrows(CynemmaServiceUnavailableException.class, () ->
                executor.executeStringRequest("/api/error/503", "GET", null, null, null));
    }

    @Test
    @DisplayName("Connection failure maps to CynemmaNetworkException")
    void connectionFailureMapsToNetworkException() {
        // Port 1 is reserved and unconnectable
        HttpRequestExecutor deadExecutor = new HttpRequestExecutor(
                "http://127.0.0.1:1/cynemma-api",
                HttpClient.newHttpClient(),
                Duration.ofMillis(200),
                null
        );

        assertThrows(CynemmaNetworkException.class, () ->
                deadExecutor.executeStringRequest("/api/health", "GET", null, null, null));
    }

    @Test
    @DisplayName("Successful GET bytes response with status 200")
    void successfulGetBytes() {
        var response = executor.executeBinaryRequest("/api/poster", null, "image/jpeg");
        assertEquals(200, response.statusCode());
        assertArrayEquals(new byte[]{0x12, 0x34, 0x56}, response.body());
    }
}
