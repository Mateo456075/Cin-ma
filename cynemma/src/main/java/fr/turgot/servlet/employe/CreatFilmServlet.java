package fr.turgot.servlet.employe;

import java.io.IOException;
import java.io.InputStream;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.PosterStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet(name = "/CreatFilmServlet", value = "/creerfilm")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class CreatFilmServlet extends HttpServlet {
    
    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        if (user == null || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CsrfProtection.ensureToken(request);
        getServletContext().getRequestDispatcher("/WEB-INF/Employer/CrerFilm.jsp").forward(request, response);
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

        String titre = request.getParameter("titre");
        String realisateur = request.getParameter("realisateur");
        String anneeStr = request.getParameter("annee");
        String genre = request.getParameter("genre");
        String tempsStr = request.getParameter("temps");

        Part filePart = request.getPart("affiche");

        if (titre == null || titre.trim().isEmpty() ||
            realisateur == null || realisateur.trim().isEmpty() ||
            anneeStr == null || anneeStr.trim().isEmpty() ||
            tempsStr == null || tempsStr.trim().isEmpty() ||
            genre == null || genre.trim().isEmpty() ||
            filePart == null || filePart.getSize() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tous les champs sont obligatoires.");
            return;
        }

        int annee;
        int temps;
        try {
            annee = Integer.parseInt(anneeStr);
            temps = Integer.parseInt(tempsStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Année ou durée invalide.");
            return;
        }

        String cheminImagePourBDD;
        try (InputStream posterInput = filePart.getInputStream()) {
            cheminImagePourBDD = PosterStorage.save(posterInput);
        } catch (PosterStorage.InvalidImageException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (PosterStorage.StorageConfigurationException e) {
            getServletContext().log("Le stockage persistant des affiches n'est pas configuré.", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Le stockage des affiches n'est pas configuré.");
            return;
        } catch (IOException e) {
            getServletContext().log("Impossible d'enregistrer l'affiche téléversée.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer l'affiche.");
            return;
        }
        Film nouveauFilm = new Film(titre, realisateur, annee, genre, cheminImagePourBDD, temps);
        if (filmDAO.create(nouveauFilm) == null) {
            try {
                PosterStorage.delete(cheminImagePourBDD, getServletContext());
            } catch (IOException e) {
                getServletContext().log("Impossible de supprimer l'affiche après l'échec de persistance.", e);
            }
            throw new ServletException("Impossible d'enregistrer le film.");
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }
}
