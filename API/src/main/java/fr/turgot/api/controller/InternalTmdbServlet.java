package fr.turgot.api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import fr.turgot.api.dto.TmdbMovie;
import fr.turgot.api.security.JwtTokenVerifier;
import fr.turgot.api.service.TmdbService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/internal/tmdb/*")
public class InternalTmdbServlet extends HttpServlet {
    private static final int MAX_REQUEST_BYTES = 4096;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!authorize(request, response)) {
            return;
        }

        String path = request.getPathInfo();
        try {
            if ("/now-playing".equals(path)) {
                List<TmdbMovie> movies = TmdbService.getNowPlayingMovies();
                JSONArray results = new JSONArray();
                movies.forEach(movie -> results.put(movieJson(movie)));
                writeJson(response, HttpServletResponse.SC_OK,
                        new JSONObject().put("results", results));
                return;
            }

            String[] segments = path == null ? new String[0] : path.split("/");
            if (segments.length == 3 && segments[1].equals("movies")
                    && segments[2].matches("[1-9][0-9]*")) {
                writeJson(response, HttpServletResponse.SC_OK,
                        movieJson(TmdbService.getMovieDetails(parseMovieId(segments[2]))));
                return;
            }

            if (segments.length == 4 && segments[1].equals("movies")
                    && segments[2].matches("[1-9][0-9]*") && segments[3].equals("poster")) {
                writePoster(parseMovieId(segments[2]), response);
                return;
            }
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Route TMDB interne introuvable");
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Identifiant TMDB invalide");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "La recherche TMDB a été interrompue");
        } catch (TmdbService.TmdbApiException e) {
            logTmdbFailure(e);
            writeError(response, e.isTimeout()
                    ? HttpServletResponse.SC_GATEWAY_TIMEOUT
                    : HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible");
        } catch (IOException e) {
            logTmdbFailure(e);
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible");
        } catch (RuntimeException e) {
            getServletContext().log("Erreur interne pendant le traitement de la requête TMDB.", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!authorize(request, response)) {
            return;
        }

        String[] segments = request.getPathInfo() == null
                ? new String[0]
                : request.getPathInfo().split("/");
        if (segments.length != 4 || !segments[1].equals("movies")
                || !segments[2].matches("[1-9][0-9]*")
                || !segments[3].equals("trailer-cache")) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Route TMDB interne introuvable");
            return;
        }

        int movieId;
        try {
            movieId = parseMovieId(segments[2]);
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Identifiant TMDB invalide");
            return;
        }

        try {
            byte[] body = request.getInputStream().readNBytes(MAX_REQUEST_BYTES + 1);
            if (body.length > MAX_REQUEST_BYTES) {
                writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "Corps de requête trop volumineux");
                return;
            }
            String contentType = request.getContentType();
            if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT)
                    .startsWith("application/json")) {
                writeError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                        "Le corps doit être en JSON");
                return;
            }
            JSONObject payload = new JSONObject(new String(body, StandardCharsets.UTF_8));
            String title = payload.optString("title", "");
            Optional<String> key = TmdbService.getTrailerKeyForMovie(movieId, title);
            writeJson(response, HttpServletResponse.SC_OK,
                    new JSONObject().put("available", key.isPresent()));
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Titre de film invalide");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "La recherche TMDB a été interrompue");
        } catch (TmdbService.TmdbApiException e) {
            logTmdbFailure(e);
            writeError(response, e.isTimeout()
                    ? HttpServletResponse.SC_GATEWAY_TIMEOUT
                    : HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible");
        } catch (SQLException e) {
            getServletContext().log("Impossible d'enregistrer le cache de bande-annonce.", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne");
        } catch (IOException e) {
            logTmdbFailure(e);
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible");
        } catch (RuntimeException e) {
            getServletContext().log("Erreur interne pendant le traitement du cache TMDB.", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne");
        }
    }

    private boolean authorize(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        switch (JwtTokenVerifier.authorizeEmployee(request)) {
            case AUTHORIZED -> {
                return true;
            }
            case FORBIDDEN -> writeError(
                    response, HttpServletResponse.SC_FORBIDDEN, "Rôle employé requis");
            case NOT_CONFIGURED -> writeError(
                    response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "La vérification des jetons n'est pas configurée");
            case UNAUTHORIZED -> writeError(
                    response, HttpServletResponse.SC_UNAUTHORIZED, "Jeton d'accès invalide");
        }
        return false;
    }

    private void writePoster(int tmdbId, HttpServletResponse response)
            throws IOException, InterruptedException {
        TmdbMovie movie = TmdbService.getMovieDetails(tmdbId);
        if (movie.posterPath() == null || movie.posterPath().isBlank()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Aucune affiche disponible");
            return;
        }

        byte[] poster = TmdbService.downloadPoster(movie.posterPath());
        String contentType = movie.posterPath().toLowerCase(java.util.Locale.ROOT).endsWith(".png")
                ? "image/png"
                : "image/jpeg";
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType);
        response.setContentLength(poster.length);
        response.getOutputStream().write(poster);
    }

    private JSONObject movieJson(TmdbMovie movie) {
        return new JSONObject()
                .put("tmdbId", movie.tmdbId())
                .put("title", movie.title())
                .put("overview", movie.overview())
                .put("releaseDate", movie.releaseDate())
                .put("posterPath", movie.posterPath() == null
                        ? JSONObject.NULL : movie.posterPath())
                .put("posterUrl", movie.posterPath() == null
                        ? JSONObject.NULL : "https://image.tmdb.org/t/p/w500" + movie.posterPath())
                .put("runtimeMinutes", movie.runtimeMinutes() == null
                        ? JSONObject.NULL : movie.runtimeMinutes())
                .put("director", movie.director())
                .put("genres", movie.genres());
    }

    private int parseMovieId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identifiant TMDB hors limite.", e);
        }
    }

    private void logTmdbFailure(Exception e) {
        getServletContext().log("Échec de la communication avec TMDB.", e);
    }

    private void writeJson(HttpServletResponse response, int status, JSONObject body)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(body.toString());
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        writeJson(response, status, new JSONObject().put("erreur", message));
    }
}
