package fr.turgot.servlet.admin;

import java.io.IOException;

import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.model.ConfigurationSalle;
import fr.turgot.dao.model.DispositionSieges;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.TypeSalle;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/CreatSalleServlet", value = "/creersalle")
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

        // On passe les listes d'enums au JSP pour remplir les <select>
        request.setAttribute("typesSalle", TypeSalle.values());
        request.setAttribute("dispositions", DispositionSieges.values());

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

        String nom            = request.getParameter("nom");
        String capaciteStr    = request.getParameter("capacite");
        String typeSalleStr   = request.getParameter("typeSalle");
        String dispositionStr = request.getParameter("disposition");
        String balconsStr     = request.getParameter("nombreBalcons");

        if (nom == null || nom.trim().isEmpty()
                || capaciteStr == null || capaciteStr.trim().isEmpty()
                || typeSalleStr == null || dispositionStr == null || balconsStr == null) {
            response.sendRedirect(request.getContextPath() + "/creerSalle");
            return;
        }

        try {
            int capacite              = Integer.parseInt(capaciteStr.trim());
            int nombreBalcons         = Integer.parseInt(balconsStr.trim());
            TypeSalle typeSalle       = TypeSalle.valueOf(typeSalleStr);
            DispositionSieges disp    = DispositionSieges.valueOf(dispositionStr);

            // 1. Créer la configuration de salle
            ConfigurationSalle config = new ConfigurationSalle();
            config.setDisposition(disp);
            config.setNombreBalcons(nombreBalcons);

            // 2. Créer la salle et lui attacher la configuration + le type
            Salle nouvelleSalle = new Salle(nom.trim(), capacite);
            nouvelleSalle.setTypeSalle(typeSalle);
            nouvelleSalle.setConfiguration(config);

            salleDAO.createAvecConfiguration(nouvelleSalle, config);

        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath() + "/creerSalle");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admins");
    }
}
