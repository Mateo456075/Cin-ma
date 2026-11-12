package fr.turgot.servlet.client;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.SeanceDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Seance;
import fr.turgot.dao.model.Utilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/SeanceListClientServlet", value = "/client/listeSeances")
public class SeanceListClientServlet extends HttpServlet {

    private final SeanceDAO SeanceDAO = new SeanceDAO();
    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
    if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
        response.sendRedirect(request.getContextPath() + "/index.jsp");
        return;
    }

    String filmIdParam = request.getParameter("filmId");
    if (filmIdParam == null) {
        response.sendRedirect(request.getContextPath() + "/client");
        return;
    }

    int filmId = Integer.parseInt(filmIdParam);
    Film film = filmDAO.getFilmById(filmId);

    List<Seance> seances = SeanceDAO.getSeancesByFilmId(filmId);

    request.setAttribute("film", film);
    request.setAttribute("seances", seances);

    getServletContext().getRequestDispatcher("/WEB-INF/client/seance.jsp").forward(request, response);
}
}
