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

    private final FilmDAO filmDAO;

    public EditFilmServlet() {
        this(new FilmDAO());
    }

    EditFilmServlet(FilmDAO filmDAO) {
        this.filmDAO = filmDAO;
    }

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

        int id;
        int annee;
        int temps;
        try {
            id = Integer.parseInt(request.getParameter("id"));
            annee = Integer.parseInt(request.getParameter("annee"));
            temps = Integer.parseInt(request.getParameter("temps"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identifiant, année ou durée invalide.");
            return;
        }

        Film film = filmDAO.getFilmById(id);
        if (film == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Film introuvable.");
            return;
        }
        film.setTitre(request.getParameter("titre"));
        film.setRealisateur(request.getParameter("realisateur"));
        film.setAnnee(annee);
        film.setGenre(request.getParameter("genre"));
        film.setDureeMinutes(temps);
        film.setAffiche(request.getParameter("ancienneAffiche"));
        if (!filmDAO.update(film)) {
            throw new ServletException("Impossible de modifier le film.");
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }
}