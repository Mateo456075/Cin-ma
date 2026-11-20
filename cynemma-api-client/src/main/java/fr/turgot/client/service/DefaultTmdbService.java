package fr.turgot.client.service;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.turgot.client.dto.TmdbMovie;
import fr.turgot.client.exception.CynemmaValidationException;
import fr.turgot.client.http.HttpRequestExecutor;

public class DefaultTmdbService implements TmdbService {
    private final HttpRequestExecutor executor;

    public DefaultTmdbService(HttpRequestExecutor executor) {
        this.executor = executor;
    }

    @Override
    public List<TmdbMovie> getNowPlaying() {
        return getNowPlaying(null);
    }

    @Override
    public List<TmdbMovie> getNowPlaying(String employeeToken) {
        HttpResponse<String> response = executor.executeStringRequest(
                "/api/internal/tmdb/now-playing", "GET", null, employeeToken, "application/json");
        JSONObject json = new JSONObject(response.body());
        JSONArray array = json.getJSONArray("results");
        List<TmdbMovie> movies = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            movies.add(toMovie(array.getJSONObject(i)));
        }
        return List.copyOf(movies);
    }

    @Override
    public TmdbMovie getMovieDetails(int tmdbId) {
        return getMovieDetails(tmdbId, null);
    }

    @Override
    public TmdbMovie getMovieDetails(int tmdbId, String employeeToken) {
        validateTmdbId(tmdbId);
        HttpResponse<String> response = executor.executeStringRequest(
                "/api/internal/tmdb/movies/" + tmdbId, "GET", null, employeeToken, "application/json");
        return toMovie(new JSONObject(response.body()));
    }

    @Override
    public byte[] downloadPoster(int tmdbId) {
        return downloadPoster(tmdbId, null);
    }

    @Override
    public byte[] downloadPoster(int tmdbId, String employeeToken) {
        validateTmdbId(tmdbId);
        HttpResponse<byte[]> response = executor.executeBinaryRequest(
                "/api/internal/tmdb/movies/" + tmdbId + "/poster",
                employeeToken,
                "image/jpeg, image/png");

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!isSupportedImageContentType(contentType)) {
            throw new CynemmaValidationException(
                    response.statusCode(),
                    "L'API n'a pas retourné une image valide JPEG ou PNG (reçu : " + contentType + ").",
                    null);
        }
        return response.body();
    }

    @Override
    public void cacheTrailer(int tmdbId, String title) {
        cacheTrailer(tmdbId, title, null);
    }

    @Override
    public void cacheTrailer(int tmdbId, String title, String employeeToken) {
        validateTmdbId(tmdbId);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre du film ne peut pas être vide.");
        }
        JSONObject payload = new JSONObject().put("title", title.trim());
        executor.executeStringRequest(
                "/api/internal/tmdb/movies/" + tmdbId + "/trailer-cache",
                "POST",
                payload.toString(),
                employeeToken,
                "application/json");
    }

    private void validateTmdbId(int tmdbId) {
        if (tmdbId <= 0) {
            throw new IllegalArgumentException("L'identifiant TMDB doit être un entier positif.");
        }
    }

    private TmdbMovie toMovie(JSONObject json) {
        int id = json.has("tmdbId") ? json.getInt("tmdbId") : json.getInt("id");
        String releaseDate = json.has("releaseDate") ? json.optString("releaseDate") : json.optString("release_date");
        String posterPath = json.has("posterPath")
                ? (json.isNull("posterPath") ? null : json.optString("posterPath"))
                : (json.isNull("poster_path") ? null : json.optString("poster_path"));
        Integer runtime = json.has("runtimeMinutes")
                ? (json.isNull("runtimeMinutes") ? null : json.optInt("runtimeMinutes"))
                : (json.has("runtime") && !json.isNull("runtime") ? json.optInt("runtime") : null);

        return new TmdbMovie(
                id,
                json.optString("title"),
                json.optString("overview"),
                releaseDate,
                posterPath,
                runtime,
                json.optString("director"),
                json.optString("genres"));
    }

    public static boolean isSupportedImageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase();
        return mediaType.equals("image/jpeg") || mediaType.equals("image/png");
    }
}
