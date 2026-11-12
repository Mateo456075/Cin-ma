package fr.turgot.servlet;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "AccueilServlet", value = "/accueil")
public class AccueilServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. Le Contrôleur interroge le DAO (Ton code vient ici)
        FilmDAO filmDAO = new FilmDAO();
        List<Film> films = filmDAO.getAllFilms();
        
        // 2. Il stocke la liste dans la requête sous le nom "films"
        request.setAttribute("films", films);
        
        // 3. Il transfère le tout à la vue (ta page index.jsp)
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}