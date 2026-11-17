package fr.turgot.servlet.api;

import java.io.IOException;
import java.util.Optional;

import org.json.JSONObject;

import fr.turgot.service.TmdbService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/trailer")
public class TrailerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String film = request.getParameter("film");
        if (film == null || film.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            write(response, new JSONObject()
                    .put("erreur", "Paramètre 'film' manquant").toString());
            return;
        }

        try {
            Optional<String> key = TmdbService.getTrailerKey(film);
            if (key.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                write(response, new JSONObject()
                        .put("trailer", JSONObject.NULL)
                        .put("message", "Aucune bande-annonce disponible").toString());
                return;
            }
            write(response, new JSONObject()
                    .put("trailer", "https://www.youtube.com/watch?v=" + key.get()).toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            write(response, new JSONObject()
                    .put("erreur", "La recherche de bande-annonce a été interrompue").toString());
        } catch (Exception e) {
            getServletContext().log("Échec de la recherche de bande-annonce TMDB.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            write(response, new JSONObject().put("erreur", "Erreur interne").toString());
        }
    }

    private void write(HttpServletResponse response, String json) throws IOException {
        response.getWriter().write(json);
    }
}
