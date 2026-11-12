package fr.turgot.servlet;

import java.io.IOException;

import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/CreatServlet", value = "/nouveaucompte")
public class CreatServlet extends HttpServlet {

    public static final String JSP_ROOT_AUTH = LinkHelper.JSP_ROOT ;
    private final UserDAO dao = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getRequestDispatcher(JSP_ROOT_AUTH + "Creat.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("nom");
        String password = request.getParameter("mdp");
        String confirmPassword = request.getParameter("confirm_mdp");
        String email = request.getParameter("email");
        String login = request.getParameter("login");
        StringBuilder messageErreur = new StringBuilder();

            // Validation des champs
        if (username == null || username.trim().isEmpty()) {
            messageErreur.append("Username manquant.<br>");
        }
        if (login == null || login.trim().isEmpty()) {
            messageErreur.append("Login manquant.<br>");
        }
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,7}$";
        if (email == null || email.trim().isEmpty() || !email.matches(emailRegex)) {
            messageErreur.append("Email invalide.<br>");
        }
        if (password == null || password.trim().isEmpty()) {
            messageErreur.append("Mot de passe manquant.<br>");
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            messageErreur.append("Confirmation du mot de passe manquante.<br>");
        }
        if (password.equals(confirmPassword)) {
        } else {
            messageErreur.append("Les mots de passe ne correspondent pas.<br>");
        }
        if (messageErreur.length() > 0) {
            request.setAttribute("errors", messageErreur.toString());
            getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Creat.jsp").forward(request, response);
        } else {
            Utilisateur user = dao.create(username, password, email, login);
            if (user == null) {
                messageErreur.append("Erreur lors de la création du compte.");
                request.setAttribute("errors", messageErreur.toString());
                getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Creat.jsp").forward(request, response);
            } else {
                request.setAttribute("user", user);
                getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Authentification.jsp").forward(request, response);
            }
        }
    }
}
