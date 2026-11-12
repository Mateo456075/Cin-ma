package fr.turgot.servlet.employe;

import java.io.IOException;
import java.time.LocalDateTime;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.SalleDAO;
import fr.turgot.dao.SeanceDAO;
import fr.turgot.dao.model.Seance;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/CreatSeanceServlet", value = "/nouvelleSeance")
public class CreatSeanceServlet extends HttpServlet {

    private final FilmDAO filmDAO = new FilmDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final SeanceDAO seanceDAO = new SeanceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        CsrfProtection.ensureToken(request);
        request.setAttribute("films", filmDAO.getAllFilms());
        request.setAttribute("salles", salleDAO.getAllSalles());

        getServletContext().getRequestDispatcher("/WEB-INF/Employer/CrerSeance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }

        try {
            int filmId = Integer.parseInt(request.getParameter("filmId"));
            int salleId = Integer.parseInt(request.getParameter("salleId"));
            String dateStr = request.getParameter("date");
            String horaireStr = request.getParameter("horaire");
            LocalDateTime nouveauDebut = LocalDateTime.parse(dateStr + "T" + horaireStr);
            if (seanceDAO.isSalleOccupee(salleId, nouveauDebut, filmId)) {
                request.setAttribute("errorMsg", "Erreur : La salle est déjà occupée ou en nettoyage sur ce créneau !");
            } else {
                Seance nouvelleSeance = new Seance(nouveauDebut, filmId, salleId);
                seanceDAO.create(nouvelleSeance);
                request.setAttribute("successMsg", "La séance a été créée avec succès !");
            }

        } catch (NumberFormatException | java.time.DateTimeException e) {
            request.setAttribute("errorMsg", "Une erreur est survenue lors du traitement.");
        }
        CsrfProtection.ensureToken(request);
        request.setAttribute("films", filmDAO.getAllFilms());
        request.setAttribute("salles", salleDAO.getAllSalles());
        getServletContext().getRequestDispatcher("/WEB-INF/Employer/CrerSeance.jsp").forward(request, response);
    }
}