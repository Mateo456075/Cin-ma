package fr.turgot.api.controller;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpResponse;

import fr.turgot.service.TmdbApiClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/trailer")
public class TrailerServlet extends HttpServlet {
    private final TmdbApiClient apiClient = new TmdbApiClient();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String film = request.getParameter("film");
        if (film == null || film.isBlank()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Paramètre 'film' manquant");
            return;
        }
        String year = request.getParameter("year");
        if (year != null && !year.isBlank()) {
            try {
                int parsedYear = Integer.parseInt(year);
                if (parsedYear < 1888 || parsedYear > 9999) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Paramètre 'year' invalide");
                return;
            }
        }
        try {
            HttpResponse<String> apiResponse = apiClient.getTrailer(
                    film, year);
            response.setStatus(apiResponse.statusCode());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(apiResponse.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Le service des bandes-annonces est temporairement indisponible");
        } catch (HttpTimeoutException e) {
            getServletContext().log("Le délai de réponse de l'API TMDB est dépassé.", e);
            writeError(response, HttpServletResponse.SC_GATEWAY_TIMEOUT,
                    "Le service des bandes-annonces ne répond pas à temps");
        } catch (IOException | IllegalStateException e) {
            getServletContext().log("L'API de bandes-annonces est indisponible.", e);
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Le service des bandes-annonces est temporairement indisponible");
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                new org.json.JSONObject().put("erreur", message).toString());
    }
}
