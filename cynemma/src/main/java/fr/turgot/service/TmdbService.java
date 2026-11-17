package fr.turgot.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.turgot.dao.TmdbCacheDAO;
import fr.turgot.dao.TmdbCacheDAO.CacheEntry;
import fr.turgot.dto.TmdbMovie;

public class TmdbService {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final int MAX_POSTER_BYTES = 10 * 1024 * 1024;
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

    public static List<TmdbMovie> getNowPlayingMovies()
            throws IOException, InterruptedException {
        String apiKey = getApiKey();
        String url = BASE_URL + "/movie/now_playing?api_key=" + encode(apiKey)
                + "&language=fr-FR&page=1";
        JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
        List<TmdbMovie> movies = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            int tmdbId = item.optInt("id");
            String title = item.optString("title");
            if (tmdbId <= 0 || title.isBlank()) {
                continue;
            }
            movies.add(new TmdbMovie(
                    tmdbId,
                    title,
                    item.optString("overview"),
                    item.optString("release_date"),
                    item.optString("poster_path", null),
                    null,
                    null,
                    null));
        }
        return List.copyOf(movies);
    }

    public static TmdbMovie getMovieDetails(int tmdbId)
            throws IOException, InterruptedException {
        if (tmdbId <= 0) {
            throw new IllegalArgumentException("L'identifiant TMDB doit être positif.");
        }

        String url = BASE_URL + "/movie/" + tmdbId + "?api_key=" + encode(getApiKey())
                + "&language=fr-FR&append_to_response=credits";
        JSONObject details = new JSONObject(fetch(url));
        JSONObject credits = details.optJSONObject("credits");
        JSONArray crew = credits == null ? new JSONArray() : credits.optJSONArray("crew");
        String director = "";
        if (crew != null) {
            for (int i = 0; i < crew.length(); i++) {
                JSONObject member = crew.optJSONObject(i);
                if (member != null && "Director".equals(member.optString("job"))) {
                    director = member.optString("name");
                    break;
                }
            }
        }

        JSONArray genres = details.optJSONArray("genres");
        String genreNames = "";
        if (genres != null) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < genres.length(); i++) {
                JSONObject genre = genres.optJSONObject(i);
                if (genre != null && !genre.optString("name").isBlank()) {
                    names.add(genre.optString("name"));
                }
            }
            genreNames = String.join(", ", names);
        }

        int runtime = details.optInt("runtime");
        return new TmdbMovie(
                details.getInt("id"),
                details.optString("title"),
                details.optString("overview"),
                details.optString("release_date"),
                details.optString("poster_path", null),
                runtime > 0 ? runtime : null,
                director,
                genreNames);
    }

    public static Optional<String> getTrailerKeyForMovie(int tmdbId, String filmTitle)
            throws IOException, InterruptedException, SQLException {
        if (tmdbId <= 0) {
            throw new IllegalArgumentException("L'identifiant TMDB doit être positif.");
        }

        String normalizedTitle = normalizeTitle(filmTitle);
        Optional<CacheEntry> cached = CACHE_DAO.findByFilmTitle(normalizedTitle);
        if (cached.isPresent() && Objects.equals(cached.get().tmdbId(), tmdbId)) {
            return Optional.ofNullable(cached.get().trailerKey());
        }

        String apiKey = getApiKey();
        String trailerKey = findTrailerKey(tmdbId, "fr-FR", apiKey);
        if (trailerKey == null) {
            trailerKey = findTrailerKey(tmdbId, "en-US", apiKey);
        }

        CACHE_DAO.save(normalizedTitle, tmdbId, trailerKey);
        return Optional.ofNullable(trailerKey);
    }

    public static byte[] downloadPoster(String posterPath)
            throws IOException, InterruptedException {
        if (posterPath == null || !posterPath.matches("/[A-Za-z0-9_-]+\\.(jpg|jpeg|png)")) {
            throw new IOException("Le chemin d'affiche TMDB est absent ou invalide.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IMAGE_BASE_URL + posterPath))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<InputStream> response =
                HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream input = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Le serveur d'affiches a répondu avec le statut HTTP "
                        + response.statusCode());
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            if (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png")) {
                throw new IOException("Le serveur TMDB n'a pas retourné une image JPEG ou PNG.");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_POSTER_BYTES) {
                    throw new IOException("L'affiche TMDB dépasse la taille maximale autorisée.");
                }
                output.write(buffer, 0, bytesRead);
            }
            return output.toByteArray();
        }
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
