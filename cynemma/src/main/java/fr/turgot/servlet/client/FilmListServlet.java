package fr.turgot.servlet.client;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/FilmListServlet", value = "/listeFilms")
public class FilmListServlet extends HttpServlet {
    
    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        CsrfProtection.ensureToken(request);
        List<Film> films = filmDAO.getAllFilms();
        request.setAttribute("films", films);
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Employer/FilmE.jsp").forward(request, response);
    }
}