package fr.turgot.servlet;

import java.io.IOException;

import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/ChangePasswordServlet", value = "/changerMotDePasse")
public class ChangePasswordServlet extends HttpServlet {

    private final UserDAO dao = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Sécurité : on vérifie que l'utilisateur est bien connecté
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        CsrfProtection.ensureToken(request);
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "ChangePassword.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }
        CsrfProtection.ensureToken(request);

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        StringBuilder messageErreur = new StringBuilder();

        // Validation des champs
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            messageErreur.append("Ancien mot de passe manquant.<br>");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            messageErreur.append("Nouveau mot de passe manquant.<br>");
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            messageErreur.append("Confirmation du mot de passe manquante.<br>");
        }
        if (newPassword != null && confirmPassword != null && !newPassword.equals(confirmPassword)) {
            messageErreur.append("Les nouveaux mots de passe ne correspondent pas.<br>");
        }

        if (messageErreur.length() == 0) {
            // On vérifie que l'ancien mot de passe est bien le bon
            Utilisateur authUser = dao.authenticate(user.getLogin(), oldPassword);
            if (authUser == null) {
                messageErreur.append("L'ancien mot de passe est incorrect.<br>");
            } else if (dao.updatePassword(user.getLogin(), newPassword)) {
                request.setAttribute("success", "Votre mot de passe a été modifié avec succès !");
                getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "ChangePassword.jsp").forward(request, response);
                return;
            } else {
                messageErreur.append("Erreur lors de la mise à jour du mot de passe en base de données.<br>");
            }
        }

        request.setAttribute("errors", messageErreur.toString());
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "ChangePassword.jsp").forward(request, response);
    }
}