package fr.turgot.api.controller;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONObject;

import fr.turgot.api.dao.TmdbCacheDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    private final TmdbCacheDAO cacheDAO = new TmdbCacheDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            cacheDAO.checkConnection();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(new JSONObject()
                    .put("status", "UP")
                    .put("database", "UP")
                    .toString());
        } catch (SQLException e) {
            getServletContext().log("La vérification de disponibilité de la base a échoué.", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write(new JSONObject()
                    .put("status", "DOWN")
                    .put("database", "DOWN")
                    .toString());
        }
    }
}
