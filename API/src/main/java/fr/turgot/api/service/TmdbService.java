package fr.turgot.api.service;

import fr.turgot.api.config.LocalConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.turgot.api.dao.TmdbCacheDAO;
import fr.turgot.api.dao.TmdbCacheDAO.CacheEntry;
import fr.turgot.api.dto.TmdbMovie;

public class TmdbService {

    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final int MAX_POSTER_BYTES = 10 * 1024 * 1024;
    private static final Object[] TRAILER_LOCKS = createTrailerLocks();
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
            throws TmdbApiException, InterruptedException, SQLException {
        return getTrailerKey(filmTitle, null);
    }

    /**
     * Retourne la bande-annonce, en limitant la recherche à l'année fournie si elle existe.
     */
    public static Optional<String> getTrailerKey(String filmTitle, Integer year)
            throws TmdbApiException, InterruptedException, SQLException {
        String normalizedTitle = normalizeTitle(filmTitle);
        if (year != null && (year < 1888 || year > 9999)) {
            throw new IllegalArgumentException("L'année doit être comprise entre 1888 et 9999.");
        }

        String cacheTitle = cacheTitle(normalizedTitle, year);
        Object lock = TRAILER_LOCKS[Math.floorMod(
                normalizedTitle.toLowerCase(Locale.ROOT).hashCode(), TRAILER_LOCKS.length)];
        synchronized (lock) {
            Optional<CacheEntry> cached = CACHE_DAO.findByFilmTitle(cacheTitle);
            if (cached.isPresent()) {
                String cachedKey = cached.get().trailerKey();
                if (cachedKey == null || isValidYouTubeKey(cachedKey)) {
                    return Optional.ofNullable(cachedKey);
                }
            }

            String apiKey = getApiKey();
            Integer tmdbId = searchMovieId(normalizedTitle, year, apiKey);
            if (tmdbId == null) {
                CACHE_DAO.save(cacheTitle, null, null);
                return Optional.empty();
            }

            String trailerKey = findTrailerKey(tmdbId, "fr-FR", apiKey);
            if (trailerKey == null) {
                trailerKey = findTrailerKey(tmdbId, "en-US", apiKey);
            }

            CACHE_DAO.save(cacheTitle, tmdbId, trailerKey);
            return Optional.ofNullable(trailerKey);
        }
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
    private static Integer searchMovieId(String title, Integer year, String apiKey)
            throws TmdbApiException, InterruptedException {
        String url = BASE_URL + "/search/movie?api_key=" + encode(apiKey)
                + "&language=fr-FR&query=" + encode(title)
                + (year == null ? "" : "&year=" + year);
        try {
            JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
            if (results.isEmpty()) {
                return null;
            }
            return results.getJSONObject(0).getInt("id");
        } catch (JSONException e) {
            throw new TmdbApiException("TMDB a retourné une réponse de recherche invalide.", e);
        }
    }

    private static String findTrailerKey(int movieId, String language, String apiKey)
            throws TmdbApiException, InterruptedException {
        String url = BASE_URL + "/movie/" + movieId + "/videos?api_key=" + encode(apiKey)
                + "&language=" + encode(language);
        try {
            JSONArray results = new JSONObject(fetch(url)).getJSONArray("results");
            String firstTrailerKey = null;
            for (int i = 0; i < results.length(); i++) {
                JSONObject video = results.getJSONObject(i);
                if ("Trailer".equals(video.optString("type"))
                        && "YouTube".equals(video.optString("site"))) {
                    String key = video.optString("key", null);
                    if (!isValidYouTubeKey(key)) {
                        continue;
                    }
                    if (video.optBoolean("official", false)) {
                        return key;
                    }
                    if (firstTrailerKey == null) {
                        firstTrailerKey = key;
                    }
                }
            }
            return firstTrailerKey;
        } catch (JSONException e) {
            throw new TmdbApiException("TMDB a retourné une réponse vidéo invalide.", e);
        }
    }

    private static String fetch(String url) throws TmdbApiException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new TmdbApiException("Le délai de réponse de TMDB est dépassé.", true, e);
        } catch (IOException e) {
            throw new TmdbApiException("Impossible de communiquer avec TMDB.", e);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TmdbApiException(
                    "TMDB a répondu avec le statut HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String cacheTitle(String normalizedTitle, Integer year) {
        String key = year == null ? normalizedTitle : normalizedTitle + " [" + year + "]";
        if (key.length() > 255) {
            throw new IllegalArgumentException(
                    "Le titre est trop long pour être mis en cache avec cette année.");
        }
        return key;
    }

    private static boolean isValidYouTubeKey(String key) {
        return key != null && key.matches("[A-Za-z0-9_-]{11}");
    }

    private static Object[] createTrailerLocks() {
        Object[] locks = new Object[64];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private static String getApiKey() {
        return requireApiKey(LocalConfiguration.get("TMDB_API_KEY"));
    }

    static String requireApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new TmdbConfigurationException(
                    "La variable d'environnement TMDB_API_KEY doit être configurée.");
        }
        if ("METTRE_TA_CLE_TMDB_ICI".equalsIgnoreCase(apiKey.trim())
                || "REPLACE_WITH_REAL_TMDB_API_KEY".equalsIgnoreCase(apiKey.trim())) {
            throw new TmdbConfigurationException(
                    "Remplace le placeholder de TMDB_API_KEY par une vraie clé TMDB.");
        }
        return apiKey;
    }

    public static final class TmdbConfigurationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private TmdbConfigurationException(String message) {
            super(message);
        }
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

    public static final class TmdbApiException extends IOException {
        private final boolean timeout;

        private TmdbApiException(String message) {
            this(message, false, null);
        }

        private TmdbApiException(String message, Throwable cause) {
            this(message, false, cause);
        }

        private TmdbApiException(String message, boolean timeout, Throwable cause) {
            super(message, cause);
            this.timeout = timeout;
        }

        public boolean isTimeout() {
            return timeout;
        }
    }
}
