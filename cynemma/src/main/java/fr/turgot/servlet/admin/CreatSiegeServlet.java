package fr.turgot.servlet.admin;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.RangeeDAO;
import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.SiegeDAO;
import fr.turgot.dao.model.Rangee;
import fr.turgot.dao.model.Salle;
import fr.turgot.dao.model.Siege;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.LinkHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet de gestion des rangées et sièges d'une salle.
 *
 * GET  /gererSieges?salleId=X  → affiche AjouterSiege.jsp avec les rangées existantes
 * POST /gererSieges             → crée une rangée (+ ses sièges automatiquement)
 */
@WebServlet(name = "/CreatSiegeServlet", value = "/gererSieges")
public class CreatSiegeServlet extends HttpServlet {

    private final SalleDAO  salleDAO  = new SalleDAO();
    private final RangeeDAO rangeeDAO = new RangeeDAO();
    private final SiegeDAO  siegeDAO  = new SiegeDAO();

    // ------------------------------------------------------------------ GET
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur admin = (Utilisateur) request.getSession().getAttribute("usere");
        if (admin == null || !"ADMINISTRATEUR".equalsIgnoreCase(admin.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String salleIdStr = request.getParameter("salleId");
        if (salleIdStr == null || salleIdStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admins");
            return;
        }

        try {
            int salleId = Integer.parseInt(salleIdStr.trim());
            Salle salle = salleDAO.findById(salleId);
            if (salle == null) {
                response.sendRedirect(request.getContextPath() + "/admins");
                return;
            }

            List<Rangee> rangees = rangeeDAO.getByIdSalle(salleId);

            request.setAttribute("salle", salle);
            request.setAttribute("rangees", rangees);
            CsrfProtection.ensureToken(request);

            getServletContext()
                .getRequestDispatcher(LinkHelper.JSP_ROOT + "admin/AjouterSiege.jsp")
                .forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admins");
        }
    }

    // ----------------------------------------------------------------- POST
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur admin = (Utilisateur) request.getSession().getAttribute("usere");
        if (admin == null || !"ADMINISTRATEUR".equalsIgnoreCase(admin.getTypeUtilisateur())) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        String salleIdStr     = request.getParameter("salleId");
        String nomRangee      = request.getParameter("nomRangee");
        String nombreSiegesStr = request.getParameter("nombreSieges");

        if (salleIdStr == null || nomRangee == null || nomRangee.trim().isEmpty()
                || nombreSiegesStr == null) {
            response.sendRedirect(request.getContextPath() + "/admins");
            return;
        }

        try {
            int salleId     = Integer.parseInt(salleIdStr.trim());
            int nbSieges    = Integer.parseInt(nombreSiegesStr.trim());
            Salle salle     = salleDAO.findById(salleId);

            if (salle == null || nbSieges < 1) {
                response.sendRedirect(request.getContextPath() + "/admins");
                return;
            }

            // 1. Créer la rangée
            Rangee rangee = new Rangee();
            rangee.setNomRangee(nomRangee.trim().toUpperCase());
            rangee.setNombreSieges(nbSieges);
            rangee.setSalle(salle);
            rangeeDAO.create(rangee);   // ← persiste en base, rangee.id est maintenant rempli

            // 2. Créer les sièges de cette rangée automatiquement (numérotés 1 à N)
            for (int i = 1; i <= nbSieges; i++) {
                Siege siege = new Siege();
                siege.setNumero(i);
                siege.setRangee(rangee);
                siegeDAO.create(siege);
            }

            // Retour à la gestion des sièges de cette même salle
            response.sendRedirect(request.getContextPath() + "/gererSieges?salleId=" + salleId);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admins");
        }
    }
}
