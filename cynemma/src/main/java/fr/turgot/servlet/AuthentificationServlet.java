package fr.turgot.servlet;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.UserDAO;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import fr.turgot.utils.SessionRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/authentification", value = "/connexion")
public class AuthentificationServlet extends HttpServlet {
    private final UserDAO dao = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Authentification.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String login = request.getParameter("login");
        String mdp = request.getParameter("mdp");

        StringBuilder messageErreur = new StringBuilder();

        // Validation des champs
        if (login == null || login.trim().isEmpty()) {
            messageErreur.append("Login manquant.<br>");
        }
        if (mdp == null || mdp.trim().isEmpty()) {
            messageErreur.append("Mot de passe manquant.<br>");
        }

        if (messageErreur.length() > 0) {
            request.setAttribute("errors", messageErreur.toString());
            getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Authentification.jsp").forward(request, response);
        } else {
            
            Utilisateur user = dao.authenticate(login, mdp);

            if (user == null) {
                messageErreur.append("Login ou mot de passe incorrect");
                request.setAttribute("error", messageErreur.toString());
                getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "Authentification.jsp").forward(request, response);
            } else {
                // On met directement l'utilisateur complet en session pour conserver son rôle !
                request.getSession().setAttribute("usere", user);
                SessionRegistry.register(request.getSession(), user.getLogin());
                
                String role = user.getTypeUtilisateur();
                String pageCible = "visiteur.jsp";
                
                if (role != null) {
                    if (role.equalsIgnoreCase("ADMINISTRATEUR")) {
                        pageCible = "admin/admin.jsp";
                        CsrfProtection.ensureToken(request);
                        List<Utilisateur> listUsers = dao.getAllUsers();
                        request.setAttribute("listUsers", listUsers);
                    List<Salle> listSalles = new SalleDAO().getAllSalles();
                    request.setAttribute("listSalles", listSalles);
                    } else if (role.equalsIgnoreCase("CLIENT")) {
                        response.sendRedirect(request.getContextPath() + "/client");
                        return;
                    } else if (role.equalsIgnoreCase("EMPLOYE")) {
                        pageCible = "Employer/Employer.jsp";
                    }
                }
                
                // Message de débogage pour voir le chemin exact dans la console du serveur
                System.out.println("DEBUG - Tomcat va chercher le fichier suivant : " + LinkHelper.JSP_ROOT + pageCible);
                
                // Redirection vers la page correspondante au rôle
                getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + pageCible).forward(request, response);
            }
        }
    }
    
}
