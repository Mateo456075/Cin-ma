package fr.turgot.servlet.admin;

import java.io.IOException;

import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import fr.turgot.utils.SessionRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/RoleModifierServlet", value = "/modifierrole")
public class RoleModifierServlet extends HttpServlet {
    private final UserDAO dao = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur adminUser = (Utilisateur) request.getSession().getAttribute("usere");
        if (adminUser == null || !"ADMINISTRATEUR".equalsIgnoreCase(adminUser.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String login = request.getParameter("login");
        String currentRole = request.getParameter("role");
        
        request.setAttribute("userLogin", login);
        request.setAttribute("currentRole", currentRole);
        CsrfProtection.ensureToken(request);
        
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "admin/RoleModifier.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur adminUser = (Utilisateur) request.getSession().getAttribute("usere");
        if (adminUser == null || !"ADMINISTRATEUR".equalsIgnoreCase(adminUser.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        String login = request.getParameter("login");
        String newRole = request.getParameter("newRole");

        if (login == null || newRole == null || !dao.updateRole(login, newRole)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Le rôle n'a pas pu être modifié.");
            return;
        }
        SessionRegistry.invalidateSessions(login);
        response.sendRedirect(request.getContextPath() + "/admins");
    }
}