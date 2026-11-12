package fr.turgot.servlet.employe;

import java.io.IOException;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/EditFilmServlet", value = "/modifierFilm")
public class EditFilmServlet extends HttpServlet {

    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isEmpty()) {
            int id = Integer.parseInt(idStr);
            Film film = filmDAO.getFilmById(id);
            if (film != null) {
                CsrfProtection.ensureToken(request);
                request.setAttribute("film", film);
                getServletContext().getRequestDispatcher("/WEB-INF/Employer/EditFilm.jsp").forward(request, response);
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String titre = request.getParameter("titre");
            String realisateur = request.getParameter("realisateur");
            int annee = Integer.parseInt(request.getParameter("annee"));
            String genre = request.getParameter("genre");
            int temps = Integer.parseInt(request.getParameter("temps"));
            String affiche = request.getParameter("ancienneAffiche");

            Film film = new Film(titre, realisateur, annee, genre, affiche, temps);
            film.setId(id); // L'ID est indispensable pour que la mise à jour (merge) fonctionne
            filmDAO.update(film);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }
}