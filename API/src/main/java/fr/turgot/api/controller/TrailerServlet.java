package fr.turgot.api.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.json.JSONObject;

import fr.turgot.api.service.TmdbService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/trailer")
public class TrailerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String film = request.getParameter("film");
        if (film == null || film.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Paramètre 'film' manquant");
            return;
        }

        Integer year = null;
        String yearParameter = request.getParameter("year");
        try {
            year = parseOptionalYear(yearParameter);
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        try {
            Optional<String> key = TmdbService.getTrailerKey(film, year);
            if (key.isEmpty()) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Aucune bande-annonce disponible");
                return;
            }
            String trailerKey = key.get();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new JSONObject()
                    .put("key", trailerKey)
                    .put("watchUrl", "https://www.youtube.com/watch?v=" + trailerKey)
                    .put("embedUrl", "https://www.youtube-nocookie.com/embed/" + trailerKey)
                    .toString());
        } catch (TmdbService.TmdbConfigurationException e) {
            getServletContext().log("La clé TMDB n'est pas configurée dans le serveur API.", e);
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Le service des bandes-annonces n'est pas configuré");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "La recherche de bande-annonce a été interrompue");
        } catch (TmdbService.TmdbApiException e) {
            getServletContext().log("Échec de la communication avec TMDB.", e);
            writeError(response, e.isTimeout()
                    ? HttpServletResponse.SC_GATEWAY_TIMEOUT
                    : HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible");
        } catch (SQLException | IllegalStateException e) {
            getServletContext().log("Échec de la recherche de bande-annonce TMDB.", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne");
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.getWriter().write(new JSONObject().put("erreur", message).toString());
    }

    static Integer parseOptionalYear(String yearParameter) {
        if (yearParameter == null || yearParameter.isBlank()) {
            return null;
        }
        try {
            int year = Integer.parseInt(yearParameter.trim());
            if (year < 1888 || year > 9999) {
                throw new IllegalArgumentException(
                        "L'année doit être comprise entre 1888 et 9999.");
            }
            return year;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Paramètre 'year' invalide", e);
        }
    }
}
