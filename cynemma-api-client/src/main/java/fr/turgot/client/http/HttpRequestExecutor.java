package fr.turgot.client.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import fr.turgot.client.auth.TokenProvider;
import fr.turgot.client.exception.CynemmaApiException;
import fr.turgot.client.exception.CynemmaAuthenticationException;
import fr.turgot.client.exception.CynemmaForbiddenException;
import fr.turgot.client.exception.CynemmaNetworkException;
import fr.turgot.client.exception.CynemmaNotFoundException;
import fr.turgot.client.exception.CynemmaServiceUnavailableException;
import fr.turgot.client.exception.CynemmaValidationException;

/**
 * Exécuteur centralisé de requêtes HTTP pour le SDK Cynemma.
 */
public class HttpRequestExecutor {

    private final URI baseUri;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final TokenProvider defaultTokenProvider;

    public HttpRequestExecutor(
            String baseUrl,
            HttpClient httpClient,
            Duration requestTimeout,
            TokenProvider defaultTokenProvider) {
        this.baseUri = validateAndNormalizeBaseUri(baseUrl);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient ne peut pas être null");
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofSeconds(15);
        this.defaultTokenProvider = defaultTokenProvider;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public HttpResponse<String> executeStringRequest(
            String path,
            String method,
            String jsonBody,
            String explicitToken,
            String acceptHeader) {
        HttpRequest.Builder builder = createRequestBuilder(path, method, jsonBody, explicitToken);
        if (acceptHeader != null && !acceptHeader.isBlank()) {
            builder.header("Accept", acceptHeader);
        } else {
            builder.header("Accept", "application/json");
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            verifyStatusCode(response.statusCode(), response.body());
            return response;
        } catch (HttpTimeoutException e) {
            throw new CynemmaNetworkException("Le délai d'attente de l'API a expiré (timeout).", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CynemmaNetworkException("L'appel vers l'API a été interrompu.", e);
        } catch (IOException e) {
            throw new CynemmaNetworkException("Erreur d'entrée/sortie lors de la communication avec l'API.", e);
        }
    }

    public HttpResponse<byte[]> executeBinaryRequest(
            String path,
            String explicitToken,
            String acceptHeader) {
        HttpRequest.Builder builder = createRequestBuilder(path, "GET", null, explicitToken);
        if (acceptHeader != null && !acceptHeader.isBlank()) {
            builder.header("Accept", acceptHeader);
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = new String(response.body(), StandardCharsets.UTF_8);
                verifyStatusCode(response.statusCode(), errorBody);
            }
            return response;
        } catch (HttpTimeoutException e) {
            throw new CynemmaNetworkException("Le délai d'attente de téléchargement a expiré.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CynemmaNetworkException("Le téléchargement depuis l'API a été interrompu.", e);
        } catch (IOException e) {
            throw new CynemmaNetworkException("Erreur réseau pendant le téléchargement binaire.", e);
        }
    }

    private HttpRequest.Builder createRequestBuilder(
            String path,
            String method,
            String jsonBody,
            String explicitToken) {
        URI resolvedUri = resolvePath(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolvedUri)
                .timeout(requestTimeout);

        String token = explicitToken != null && !explicitToken.isBlank()
                ? explicitToken
                : (defaultTokenProvider != null ? defaultTokenProvider.getToken() : null);

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }

        if ("POST".equalsIgnoreCase(method)) {
            String payload = jsonBody != null ? jsonBody : "";
            builder.header("Content-Type", "application/json; charset=UTF-8");
            builder.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        } else if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            throw new IllegalArgumentException("Méthode HTTP non supportée : " + method);
        }

        return builder;
    }

    public URI resolvePath(String path) {
        String base = baseUri.toString();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + normalizedPath);
    }

    private void verifyStatusCode(int status, String body) {
        if (status >= 200 && status < 300) {
            return;
        }

        String errorMessage = extractErrorMessage(status, body);

        switch (status) {
            case 400, 422 -> throw new CynemmaValidationException(status, errorMessage, body);
            case 401 -> throw new CynemmaAuthenticationException(errorMessage, body);
            case 403 -> throw new CynemmaForbiddenException(errorMessage, body);
            case 404 -> throw new CynemmaNotFoundException(errorMessage, body);
            case 502, 503, 504 -> throw new CynemmaServiceUnavailableException(status, errorMessage, body);
            default -> throw new CynemmaApiException(status, errorMessage, body);
        }
    }

    private String extractErrorMessage(int status, String body) {
        if (body != null && !body.isBlank()) {
            try {
                JSONObject json = new JSONObject(body);
                String err = json.optString("erreur", json.optString("message", ""));
                if (!err.isBlank()) {
                    return err;
                }
            } catch (JSONException ignored) {
                // Corps non-JSON (ex: page HTML d'erreur 500 générique de conteneur)
            }
        }
        return "L'API Cynemma a retourné une erreur HTTP " + status + ".";
    }

    public static URI validateAndNormalizeBaseUri(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("L'URL de base de l'API est obligatoire.");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("L'URL de base de l'API est invalide : " + baseUrl, e);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || host == null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("L'URL de base doit être une URL HTTP(S) absolue valide sans userInfo : " + baseUrl);
        }

        if ("http".equalsIgnoreCase(scheme) && !isLoopbackHost(host)) {
            throw new IllegalArgumentException(
                    "Le protocole HTTP non sécurisé est restreint aux adresses de boucle locale (localhost, 127.0.0.1, ::1). " +
                    "Utilisez HTTPS pour : " + baseUrl);
        }

        return uri;
    }

    public static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        return normalizedHost.equals("localhost")
                || normalizedHost.equals("127.0.0.1")
                || normalizedHost.equals("::1");
    }
}
