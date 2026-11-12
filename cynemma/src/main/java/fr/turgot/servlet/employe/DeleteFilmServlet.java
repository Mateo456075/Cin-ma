package fr.turgot.servlet.employe;

import java.io.IOException;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/DeleteFilmServlet", value = "/supprimerFilm")
public class DeleteFilmServlet extends HttpServlet {

    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isEmpty()) {
            try {
                int id = Integer.parseInt(idStr);
                boolean deleted = filmDAO.delete(id);
                if (!deleted) {
                    response.sendRedirect(request.getContextPath() + "/listeFilms?error=seance");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identifiant de film invalide.");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }
}