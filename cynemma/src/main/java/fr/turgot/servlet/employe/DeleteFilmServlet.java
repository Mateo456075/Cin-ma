package fr.turgot.servlet.employe;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "/DeleteFilmServlet", value = "/supprimerFilm")
public class DeleteFilmServlet extends HttpServlet {

    private final FilmDAO filmDAO = new FilmDAO();

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

        String idStr = request.getParameter("id");
        if (idStr != null && !idStr.isEmpty()) {
            try {
                int id = Integer.parseInt(idStr);
                Film film = filmDAO.getFilmById(id);
                boolean deleted = filmDAO.delete(id);
                if (!deleted) {
                    response.sendRedirect(request.getContextPath() + "/listeFilms?error=seance");
                    return;
                }
                if (film != null && !filmDAO.isAfficheUsedByAnotherFilm(film.getAffiche())) {
                    deleteUnusedPoster(film.getAffiche());
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identifiant de film invalide.");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }

    static boolean isSafePosterPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        try {
            Path path = Path.of(relativePath).normalize();
            return path.getNameCount() == 2
                    && "image".equals(path.getName(0).toString())
                    && !path.getName(1).toString().equals(".")
                    && !path.getName(1).toString().equals("..");
        } catch (InvalidPathException e) {
            return false;
        }
    }

    private void deleteUnusedPoster(String relativePath) throws IOException {
        if (!isSafePosterPath(relativePath)) {
            return;
        }
        String realPath = getServletContext().getRealPath("/");
        if (realPath == null) {
            return;
        }
        Path webRoot = Path.of(realPath).toRealPath();
        Path uploadDirectory = webRoot.resolve("image");
        if (!Files.isDirectory(uploadDirectory)) {
            return;
        }
        Path realUploadDirectory = uploadDirectory.toRealPath();
        if (!realUploadDirectory.startsWith(webRoot)) {
            return;
        }
        Path poster = webRoot.resolve(relativePath).normalize();
        if (poster.getParent() != null
                && poster.getParent().toRealPath().equals(realUploadDirectory)) {
            Files.deleteIfExists(poster);
        }
    }
}