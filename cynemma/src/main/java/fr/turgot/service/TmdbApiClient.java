package fr.turgot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.turgot.client.CynemmaApiClient;
import fr.turgot.client.dto.TrailerResponse;
import fr.turgot.client.exception.CynemmaApiException;
import fr.turgot.client.exception.CynemmaNetworkException;
import fr.turgot.client.service.DefaultTmdbService;
import fr.turgot.dto.TmdbMovie;

/**
 * Adaptateur de communication avec l'API Cynemma.
 * <p>
 * Cette classe délègue désormais tous ses appels HTTP et sa logique de transport
 * au SDK client officiel {@link CynemmaApiClient} (artefact JAR fr.turgot:cynemma-api-client).
 * </p>
 */
public class TmdbApiClient {
    private static final String BASE_URL_ENVIRONMENT_VARIABLE = "CYNEMMA_API_BASE_URL";

    private final CynemmaApiClient cynemmaApiClient;

    public TmdbApiClient() {
        this(null);
    }

    public TmdbApiClient(CynemmaApiClient client) {
        if (client != null) {
            this.cynemmaApiClient = client;
        } else {
            String baseUrl = LocalConfiguration.get(BASE_URL_ENVIRONMENT_VARIABLE);
            if (baseUrl != null && !baseUrl.isBlank()) {
                this.cynemmaApiClient = CynemmaApiClient.builder()
                        .baseUrl(baseUrl)
                        .connectTimeout(Duration.ofSeconds(5))
                        .requestTimeout(Duration.ofSeconds(15))
                        .build();
            } else {
                this.cynemmaApiClient = null;
            }
        }
    }

    private CynemmaApiClient getClient() {
        if (cynemmaApiClient != null) {
            return cynemmaApiClient;
        }
        String baseUrl = LocalConfiguration.get(BASE_URL_ENVIRONMENT_VARIABLE);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    BASE_URL_ENVIRONMENT_VARIABLE + " doit être configurée.");
        }
        return CynemmaApiClient.builder()
                .baseUrl(baseUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(15))
                .build();
    }

    public HttpResponse<String> getTrailer(String film, String year)
            throws IOException, InterruptedException {
        Integer parsedYear = null;
        if (year != null && !year.isBlank()) {
            try {
                parsedYear = Integer.parseInt(year);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            Optional<TrailerResponse> trailerOpt = getClient().trailers().getTrailer(film, parsedYear);
            if (trailerOpt.isPresent()) {
                TrailerResponse tr = trailerOpt.get();
                String json = "{\"key\":\"" + escapeJson(tr.key()) + "\",\"watchUrl\":\""
                        + escapeJson(tr.watchUrl()) + "\",\"embedUrl\":\""
                        + escapeJson(tr.embedUrl()) + "\"}";
                return new SimpleStringHttpResponse(200, json);
            } else {
                return new SimpleStringHttpResponse(404, "{\"erreur\":\"Aucune bande-annonce trouvée\"}");
            }
        } catch (CynemmaNetworkException e) {
            throw new IOException(e.getMessage(), e);
        } catch (CynemmaApiException e) {
            int code = e.getStatusCode() > 0 ? e.getStatusCode() : 500;
            String body = e.getErrorBody() != null ? e.getErrorBody() : "{\"erreur\":\"" + escapeJson(e.getMessage()) + "\"}";
            return new SimpleStringHttpResponse(code, body);
        }
    }

    public List<TmdbMovie> getNowPlayingMovies(String employeeLogin)
            throws IOException, InterruptedException {
        String token = ApiJwtTokenService.issueEmployeeToken(employeeLogin);
        try {
            List<fr.turgot.client.dto.TmdbMovie> sdkMovies = getClient().tmdb().getNowPlaying(token);
            List<TmdbMovie> movies = new ArrayList<>();
            for (fr.turgot.client.dto.TmdbMovie m : sdkMovies) {
                movies.add(toLocalMovie(m));
            }
            return List.copyOf(movies);
        } catch (CynemmaApiException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public TmdbMovie getMovieDetails(int tmdbId, String employeeLogin)
            throws IOException, InterruptedException {
        String token = ApiJwtTokenService.issueEmployeeToken(employeeLogin);
        try {
            fr.turgot.client.dto.TmdbMovie sdkMovie = getClient().tmdb().getMovieDetails(tmdbId, token);
            return toLocalMovie(sdkMovie);
        } catch (CynemmaApiException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public byte[] downloadPoster(int tmdbId, String employeeLogin)
            throws IOException, InterruptedException {
        String token = ApiJwtTokenService.issueEmployeeToken(employeeLogin);
        try {
            return getClient().tmdb().downloadPoster(tmdbId, token);
        } catch (CynemmaApiException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void cacheTrailerForMovie(int tmdbId, String title, String employeeLogin)
            throws IOException, InterruptedException {
        String token = ApiJwtTokenService.issueEmployeeToken(employeeLogin);
        try {
            getClient().tmdb().cacheTrailer(tmdbId, title, token);
        } catch (CynemmaApiException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public static boolean isSupportedImageContentType(String contentType) {
        return DefaultTmdbService.isSupportedImageContentType(contentType);
    }

    public static URI buildApiUri(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(BASE_URL_ENVIRONMENT_VARIABLE + " doit être configurée.");
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        URI uri;
        try {
            uri = URI.create(normalizedBase + path);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(BASE_URL_ENVIRONMENT_VARIABLE + " doit être une URL HTTP(S) absolue valide.", e);
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

    private static TmdbMovie toLocalMovie(fr.turgot.client.dto.TmdbMovie m) {
        return new TmdbMovie(
                m.tmdbId(),
                m.title(),
                m.overview(),
                m.releaseDate(),
                m.posterPath(),
                m.runtimeMinutes(),
                m.director(),
                m.genres()
        );
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Implémentation minimale de {@link HttpResponse} pour garantir la rétro-compatibilité
     * stricte avec {@link fr.turgot.api.controller.TrailerServlet}.
     */
    private static class SimpleStringHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;

        SimpleStringHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true);
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public java.net.http.HttpClient.Version version() {
            return java.net.http.HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
