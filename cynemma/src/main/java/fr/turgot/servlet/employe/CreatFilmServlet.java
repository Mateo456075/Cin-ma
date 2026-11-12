package fr.turgot.servlet.employe;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@WebServlet(name = "/CreatFilmServlet", value = "/creerfilm")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class CreatFilmServlet extends HttpServlet {
    
    private static final String UPLOAD_DIR = "image";
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
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
        try {
            cheminImagePourBDD = savePoster(filePart, request);
        } catch (InvalidImageException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        Film nouveauFilm = new Film(titre, realisateur, annee, genre, cheminImagePourBDD, temps);
        if (filmDAO.create(nouveauFilm) == null) {
            Files.deleteIfExists(Path.of(request.getServletContext().getRealPath("/"))
                    .resolve(cheminImagePourBDD).normalize());
            throw new ServletException("Impossible d'enregistrer le film.");
        }
        response.sendRedirect(request.getContextPath() + "/listeFilms");
    }

    private String savePoster(Part filePart, HttpServletRequest request) throws IOException, ServletException {
        String applicationPath = request.getServletContext().getRealPath("/");
        if (applicationPath == null) {
            throw new ServletException("Le conteneur ne fournit pas de chemin de stockage pour les affiches.");
        }

        String fileName = UUID.randomUUID() + ".png";
        Path uploadDirectory = Path.of(applicationPath, UPLOAD_DIR).toAbsolutePath().normalize();
        Path destination = uploadDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new ServletException("Chemin de stockage d'affiche invalide.");
        }

        try (ImageInputStream imageInput = ImageIO.createImageInputStream(filePart.getInputStream())) {
            if (imageInput == null) {
                throw new InvalidImageException("Le fichier envoyé ne peut pas être lu comme une image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Format d'image non pris en charge.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                    throw new InvalidImageException("Seules les images PNG et JPEG sont acceptées.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new InvalidImageException("Les dimensions de l'image dépassent la limite autorisée.");
                }

                BufferedImage image;
                try {
                    image = reader.read(0);
                } catch (IOException e) {
                    throw new InvalidImageException("L'image envoyée est invalide.", e);
                }
                if (image == null) {
                    throw new InvalidImageException("L'image envoyée est invalide.");
                }
                Files.createDirectories(uploadDirectory);
                try (var output = Files.newOutputStream(destination)) {
                    if (!ImageIO.write(image, "png", output)) {
                        throw new IOException("Impossible de convertir l'image reçue.");
                    }
                }
            } finally {
                reader.dispose();
            }
        }
        return UPLOAD_DIR + "/" + fileName;
    }

    private static final class InvalidImageException extends IOException {
        private InvalidImageException(String message) {
            super(message);
        }

        private InvalidImageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
