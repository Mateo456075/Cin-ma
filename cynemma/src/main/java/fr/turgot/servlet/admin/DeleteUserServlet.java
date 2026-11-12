package fr.turgot.servlet.admin;

import java.io.IOException;

import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.SessionRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/DeleteUserServlet", value = "/supprimerUtilisateur")
public class DeleteUserServlet extends HttpServlet {
    private final UserDAO dao = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur adminUser = (Utilisateur) request.getSession().getAttribute("usere");
        if (adminUser == null || !"ADMINISTRATEUR".equalsIgnoreCase(adminUser.getTypeUtilisateur())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        String login = request.getParameter("login");
        if (login != null && !login.trim().isEmpty()) {
            if (dao.deleteUser(login)) {
                SessionRegistry.invalidateSessions(login);
            }
        }
        response.sendRedirect(request.getContextPath() + "/admins");
    }
}