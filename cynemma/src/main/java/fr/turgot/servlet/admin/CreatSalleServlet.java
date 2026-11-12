package fr.turgot.servlet.admin;

import java.io.IOException;

import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/CreatSalleServlet", value = "/creerSalle")
public class CreatSalleServlet extends HttpServlet {

    private final SalleDAO salleDAO = new SalleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur adminUser = (Utilisateur) request.getSession().getAttribute("usere");
        if (adminUser == null || !"ADMINISTRATEUR".equalsIgnoreCase(adminUser.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        CsrfProtection.ensureToken(request);
        getServletContext().getRequestDispatcher(LinkHelper.JSP_ROOT + "admin/AjouterSalle.jsp").forward(request, response);
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

        String nom = request.getParameter("nom");
        String capaciteStr = request.getParameter("capacite");
        

        if (nom != null && !nom.trim().isEmpty() && capaciteStr != null && !capaciteStr.trim().isEmpty()) {
            try {
                int capacite = Integer.parseInt(capaciteStr.trim());
                Salle nouvelleSalle = new Salle(nom, capacite);
                salleDAO.create(nouvelleSalle);
            } catch (NumberFormatException e) {
            }
        }

        response.sendRedirect(request.getContextPath() + "/admins");
    }
}