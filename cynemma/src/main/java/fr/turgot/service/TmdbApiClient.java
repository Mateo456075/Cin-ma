package fr.turgot.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.turgot.dto.TmdbMovie;

public class TmdbApiClient {
    private static final String BASE_URL_ENVIRONMENT_VARIABLE = "CYNEMMA_API_BASE_URL";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public HttpResponse<String> getTrailer(String film, String year)
            throws IOException, InterruptedException {
        String path = "/api/trailer?film=" + encode(film);
        if (year != null && !year.isBlank()) {
            path += "&year=" + encode(year);
        }
        HttpRequest request = HttpRequest.newBuilder(apiUri(path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public List<TmdbMovie> getNowPlayingMovies(String employeeLogin)
            throws IOException, InterruptedException {
        JSONObject response = getJson("/api/internal/tmdb/now-playing", employeeLogin);
        JSONArray results = response.getJSONArray("results");
        List<TmdbMovie> movies = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            movies.add(toMovie(results.getJSONObject(i)));
        }
        return List.copyOf(movies);
    }

    public TmdbMovie getMovieDetails(int tmdbId, String employeeLogin)
            throws IOException, InterruptedException {
        return toMovie(getJson("/api/internal/tmdb/movies/" + positiveId(tmdbId), employeeLogin));
    }

    public byte[] downloadPoster(int tmdbId, String employeeLogin)
            throws IOException, InterruptedException {
        HttpRequest request = internalRequest(
                "/api/internal/tmdb/movies/" + positiveId(tmdbId) + "/poster", employeeLogin)
                .header("Accept", "image/jpeg, image/png")
                .GET()
                .build();
        HttpResponse<byte[]> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw apiFailure(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.equalsIgnoreCase("image/jpeg") && !contentType.equalsIgnoreCase("image/png")) {
            throw new IOException("L'API TMDB n'a pas retourné une image JPEG ou PNG.");
        }
        return response.body();
    }

    public void cacheTrailerForMovie(int tmdbId, String title, String employeeLogin)
            throws IOException, InterruptedException {
        JSONObject payload = new JSONObject().put("title", title);
        HttpRequest request = internalRequest(
                "/api/internal/tmdb/movies/" + positiveId(tmdbId) + "/trailer-cache", employeeLogin)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        requireSuccess(response.statusCode(), response.body());
    }

    private JSONObject getJson(String path, String employeeLogin)
            throws IOException, InterruptedException {
        HttpRequest request = internalRequest(path, employeeLogin)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        requireSuccess(response.statusCode(), response.body());
        return new JSONObject(response.body());
    }

    private HttpRequest.Builder internalRequest(String path, String employeeLogin) {
        String token = ApiJwtTokenService.issueEmployeeToken(employeeLogin);
        return HttpRequest.newBuilder(apiUri(path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token);
    }

    private URI apiUri(String path) {
        String baseUrl = LocalConfiguration.get(BASE_URL_ENVIRONMENT_VARIABLE);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    BASE_URL_ENVIRONMENT_VARIABLE + " doit être configurée.");
        }
        return buildApiUri(baseUrl, path);
    }

    static URI buildApiUri(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        URI uri;
        try {
            uri = URI.create(normalizedBase + path);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    BASE_URL_ENVIRONMENT_VARIABLE + " doit être une URL HTTP(S) absolue valide.", e);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || host == null || uri.getUserInfo() != null
                || ("http".equalsIgnoreCase(scheme) && !isLoopbackHost(host))) {
            throw new IllegalStateException(
                    BASE_URL_ENVIRONMENT_VARIABLE
                            + " doit être une URL HTTPS valide (HTTP est autorisé uniquement sur localhost ou une adresse loopback).");
        }
        return uri;
    }

    private static boolean isLoopbackHost(String host) {
        String normalizedHost = host.toLowerCase(java.util.Locale.ROOT);
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        return normalizedHost.equals("localhost")
                || normalizedHost.equals("127.0.0.1")
                || normalizedHost.equals("::1");
    }

    private TmdbMovie toMovie(JSONObject json) {
        return new TmdbMovie(
                json.getInt("tmdbId"),
                json.optString("title"),
                json.optString("overview"),
                json.optString("releaseDate"),
                json.isNull("posterPath") ? null : json.getString("posterPath"),
                json.isNull("runtimeMinutes") ? null : json.getInt("runtimeMinutes"),
                json.optString("director"),
                json.optString("genres"));
    }

    private String positiveId(int tmdbId) {
        if (tmdbId <= 0) {
            throw new IllegalArgumentException("L'identifiant TMDB doit être positif.");
        }
        return Integer.toString(tmdbId);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void requireSuccess(int status, String body) throws IOException {
        if (status < 200 || status >= 300) {
            throw apiFailure(status, body);
        }
    }

    private IOException apiFailure(int status, String body) {
        String message = "L'API Cynemma a répondu avec le statut HTTP " + status + ".";
        try {
            String error = new JSONObject(body).optString("erreur");
            if (!error.isBlank()) {
                message += " " + error;
            }
        } catch (RuntimeException ignored) {
            // Le corps non JSON n'est pas ajouté au message pour éviter d'exposer des détails internes.
        }
        return new IOException(message);
    }
}
