package fr.turgot.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.turgot.dao.TmdbCacheDAO;
import fr.turgot.dao.TmdbCacheDAO.CacheEntry;

public class TmdbService {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final TmdbCacheDAO CACHE_DAO = new TmdbCacheDAO();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private TmdbService() {}

    /**
     * Retourne la clé YouTube mise en cache ou la récupère auprès de TMDB.
     * Un Optional vide signifie qu'aucune bande-annonce n'est disponible.
     */
    public static Optional<String> getTrailerKey(String filmTitle)
            throws IOException, InterruptedException, SQLException {
        String normalizedTitle = normalizeTitle(filmTitle);
        Optional<CacheEntry> cached = CACHE_DAO.findByFilmTitle(normalizedTitle);
        if (cached.isPresent()) {
            return Optional.ofNullable(cached.get().trailerKey());
        }

        String apiKey = getApiKey();
        Integer tmdbId = searchMovieId(normalizedTitle, apiKey);
        if (tmdbId == null) {
            CACHE_DAO.save(normalizedTitle, null, null);
            return Optional.empty();
        }

        String trailerKey = findTrailerKey(tmdbId, "fr-FR", apiKey);
        if (trailerKey == null) {
            trailerKey = findTrailerKey(tmdbId, "en-US", apiKey);
        }

        CACHE_DAO.save(normalizedTitle, tmdbId, trailerKey);
        return Optional.ofNullable(trailerKey);
    }

    /**
     * Recherche le film correspondant au titre et retourne son identifiant TMDB.
     */
    private static Integer searchMovieId(String title, String apiKey)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/search/movie?api_key=" + encode(apiKey)
                + "&language=fr-FR&query=" + encode(title);
        JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
        if (results.isEmpty()) {
            return null;
        }
        return results.getJSONObject(0).getInt("id");
    }

    private static String findTrailerKey(int movieId, String language, String apiKey)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/movie/" + movieId + "/videos?api_key=" + encode(apiKey)
                + "&language=" + encode(language);
        JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            JSONObject video = results.getJSONObject(i);
            if ("Trailer".equals(video.optString("type"))
                    && "YouTube".equals(video.optString("site"))) {
                return video.optString("key", null);
            }
        }
        return null;
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response =
                HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("TMDB a répondu avec le statut HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String getApiKey() {
        String apiKey = System.getenv("TMDB_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("tmdb.api.key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La variable TMDB_API_KEY ou la propriété tmdb.api.key doit être configurée.");
        }
        return apiKey;
    }

    private static String normalizeTitle(String filmTitle) {
        if (filmTitle == null || filmTitle.isBlank()) {
            throw new IllegalArgumentException("Le titre du film est obligatoire.");
        }
        String normalized = filmTitle.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Le titre du film ne doit pas dépasser 255 caractères.");
        }
        return normalized;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
