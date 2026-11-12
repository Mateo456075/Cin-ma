package fr.turgot.servlet.client;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/ClientServlet", value = "/client")
public class ClientServlet extends HttpServlet {

    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Vérification de la session et du rôle
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"CLIENT".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        List<Film> films = filmDAO.getAllFilms();
        request.setAttribute("films", films);
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "client/client.jsp").forward(request, response);
    }
}