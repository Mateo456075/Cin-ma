package fr.turgot.servlet.admin;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/AdminServlet", value = "/admins")
public class AdminServlet extends HttpServlet {
    private final UserDAO dao = new UserDAO();
    private final SalleDAO salleDAO = new SalleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Vérification de la session et du rôle
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"ADMINISTRATEUR".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        CsrfProtection.ensureToken(request);
        // On charge les données nécessaires pour la vue Admin
        List<Utilisateur> listUsers = dao.getAllUsers();
        request.setAttribute("listUsers", listUsers);
        
        // On charge la liste des salles pour la vue Admin
        List<Salle> listSalles = salleDAO.getAllSalles();
        request.setAttribute("listSalles", listSalles);

        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "admin/admin.jsp").forward(request, response);
    }
}