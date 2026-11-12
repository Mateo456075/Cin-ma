package fr.turgot.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@WebServlet(name = "/SeanceListServlet", value = "/employe/listeSeances")
public class SeanceListServlet extends HttpServlet {

    private final SeanceDAO seanceDAO = new SeanceDAO();
    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        List<Seance> seances = seanceDAO.getAllSeances();
        List<Film> films = filmDAO.getAllFilms();
        
        Map<Integer, Film> filmsMap = new HashMap<>();
        for (Film f : films) {
            filmsMap.put(f.getId(), f);
        }

        request.setAttribute("seances", seances);
        request.setAttribute("filmsMap", filmsMap);

        getServletContext().getRequestDispatcher("/WEB-INF/Employer/SeanceE.jsp").forward(request, response);
    }
}