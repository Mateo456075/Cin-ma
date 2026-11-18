package fr.turgot.servlet.employe;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.dto.TmdbMovie;
import fr.turgot.service.TmdbService;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/employe/tmdb/catalogue")
public class TmdbCatalogImportServlet extends HttpServlet {

    private static final String SUGGESTIONS_JSP = "/WEB-INF/Employer/TmdbSuggestions.jsp";
    private static final String REVIEW_JSP = "/WEB-INF/Employer/TmdbReview.jsp";
    private static final String UPLOAD_DIR = "image";
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private final FilmDAO filmDAO = new FilmDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isEmployee(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CsrfProtection.ensureToken(request);

        String tmdbIdParameter = request.getParameter("tmdbId");
        try {
            if (tmdbIdParameter == null || tmdbIdParameter.isBlank()) {
                List<TmdbMovie> movies = TmdbService.getNowPlayingMovies();
                request.setAttribute("movies", movies);
                forward(request, response, SUGGESTIONS_JSP);
                return;
            }

            int tmdbId = parseTmdbId(tmdbIdParameter);
            TmdbMovie movie = TmdbService.getMovieDetails(tmdbId);
            setReviewAttributes(request, movie);
            forward(request, response, REVIEW_JSP);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identifiant TMDB invalide.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (IOException e) {
            getServletContext().log("Impossible de récupérer les films TMDB.", e);
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "Le service TMDB est temporairement indisponible.");
        } catch (RuntimeException e) {
            getServletContext().log("Erreur lors du traitement des suggestions TMDB.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isEmployee(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Jeton CSRF invalide.");
            return;
        }
        CsrfProtection.ensureToken(request);

        int tmdbId;
        try {
            tmdbId = parseTmdbId(request.getParameter("tmdbId"));
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identifiant TMDB invalide.");
            return;
        }

        String localPosterPath = null;
        try {
            TmdbMovie tmdbMovie = TmdbService.getMovieDetails(tmdbId);
            String title = parameter(request, "title");
            String director = parameter(request, "director");
            String genre = parameter(request, "genre");
            int year;
            int duration;
            try {
                year = parseInteger(request.getParameter("year"), "Année invalide.");
                duration = parseInteger(request.getParameter("duration"), "Durée invalide.");
            } catch (IllegalArgumentException e) {
                request.setAttribute("error", e.getMessage());
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            }

            String validationError = validateFilm(title, director, year, genre, duration);
            if (validationError != null) {
                request.setAttribute("error", validationError);
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            }
            if (tmdbMovie.getPosterPath() == null || tmdbMovie.getPosterPath().isBlank()) {
                request.setAttribute("error",
                        "Aucune affiche TMDB n'est disponible. Utilisez la création manuelle du film.");
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            }

            TmdbService.getTrailerKeyForMovie(tmdbId, title);
            byte[] posterBytes = TmdbService.downloadPoster(tmdbMovie.getPosterPath());
            try {
                localPosterPath = savePoster(posterBytes, request);
            } catch (IOException e) {
                getServletContext().log("Impossible d'enregistrer l'affiche TMDB localement.", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Impossible d'enregistrer l'affiche.");
                return;
            }

            Film film = new Film(title, director, year, genre, localPosterPath, duration);
            if (filmDAO.create(film) == null) {
                deletePoster(localPosterPath, request);
                localPosterPath = null;
                request.setAttribute("error", "Impossible d'enregistrer le film dans le catalogue.");
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            }

            localPosterPath = null;
            response.sendRedirect(request.getContextPath() + "/listeFilms");
        } catch (InterruptedException e) {
            cleanupPoster(localPosterPath, request);
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (IOException e) {
            cleanupPoster(localPosterPath, request);
            getServletContext().log("Erreur de communication avec TMDB pendant l'import.", e);
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "TMDB ou son serveur d'affiches est temporairement indisponible.");
        } catch (java.sql.SQLException e) {
            cleanupPoster(localPosterPath, request);
            getServletContext().log("Impossible d'enregistrer le cache de bande-annonce TMDB.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            cleanupPoster(localPosterPath, request);
            getServletContext().log("Erreur lors de l'import du film TMDB.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isEmployee(HttpServletRequest request) {
        Utilisateur user = (Utilisateur) request.getSession().getAttribute("usere");
        return user != null && "EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur());
    }

    private int parseTmdbId(String value) {
        int tmdbId = Integer.parseInt(value);
        if (tmdbId <= 0) {
            throw new IllegalArgumentException("L'identifiant TMDB doit être positif.");
        }
        return tmdbId;
    }

    private int parseInteger(String value, String errorMessage) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private String validateFilm(String title, String director, int year, String genre, int duration) {
        if (title.isBlank() || title.length() > 255) {
            return "Le titre est obligatoire et limité à 255 caractères.";
        }
        if (director.isBlank() || director.length() > 255) {
            return "Le réalisateur est obligatoire et limité à 255 caractères.";
        }
        if (year < 1895 || year > 2100) {
            return "L'année doit être comprise entre 1895 et 2100.";
        }
        if (genre.isBlank() || genre.length() > 100) {
            return "Le genre est obligatoire et limité à 100 caractères.";
        }
        if (duration <= 0) {
            return "La durée doit être un nombre positif de minutes.";
        }
        return null;
    }

    private void setReviewAttributes(HttpServletRequest request, TmdbMovie movie) {
        request.setAttribute("movie", movie);
        request.setAttribute("formTitle", formValue(request, "title", movie.getTitle()));
        request.setAttribute("formDirector", formValue(request, "director", movie.getDirector()));
        request.setAttribute("formYear", formValue(request, "year", movie.getReleaseYear()));
        request.setAttribute("formGenre", formValue(request, "genre", movie.getGenres()));
        request.setAttribute("formDuration", formValue(
                request,
                "duration",
                movie.getRuntimeMinutes() == null ? "" : movie.getRuntimeMinutes().toString()));
    }

    private String formValue(HttpServletRequest request, String parameter, String defaultValue) {
        String submittedValue = request.getParameter(parameter);
        return submittedValue == null ? (defaultValue == null ? "" : defaultValue) : submittedValue;
    }

    private String savePoster(byte[] bytes, HttpServletRequest request) throws IOException {
        String applicationPath = request.getServletContext().getRealPath("/");
        if (applicationPath == null) {
            throw new IOException("Le conteneur ne fournit pas de chemin de stockage.");
        }

        BufferedImage image;
        try (ImageInputStream imageInput =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (imageInput == null) {
                throw new IOException("L'affiche TMDB ne peut pas être lue.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Le format de l'affiche TMDB n'est pas pris en charge.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                    throw new IOException("Seules les affiches PNG et JPEG sont acceptées.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IOException("Les dimensions de l'affiche dépassent la limite autorisée.");
                }
                image = reader.read(0);
                if (image == null) {
                    throw new IOException("L'affiche TMDB est invalide.");
                }
            } finally {
                reader.dispose();
            }
        }

        Path applicationRoot = Path.of(applicationPath).toAbsolutePath().normalize();
        Path uploadDirectory = applicationRoot.resolve(UPLOAD_DIR).normalize();
        String fileName = UUID.randomUUID() + ".png";
        Path destination = uploadDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new IOException("Chemin de stockage d'affiche invalide.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            try (var output = Files.newOutputStream(destination)) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("Impossible d'enregistrer l'affiche TMDB.");
                }
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
        return UPLOAD_DIR + "/" + fileName;
    }

    private void cleanupPoster(String relativePath, HttpServletRequest request) {
        if (relativePath != null) {
            deletePoster(relativePath, request);
        }
    }

    private void deletePoster(String relativePath, HttpServletRequest request) {
        String applicationPath = request.getServletContext().getRealPath("/");
        if (applicationPath == null) {
            return;
        }
        Path applicationRoot = Path.of(applicationPath).toAbsolutePath().normalize();
        Path uploadDirectory = applicationRoot.resolve(UPLOAD_DIR).normalize();
        Path poster = applicationRoot.resolve(relativePath).normalize();
        if (poster.startsWith(uploadDirectory)) {
            try {
                Files.deleteIfExists(poster);
            } catch (IOException e) {
                getServletContext().log("Impossible de supprimer l'affiche non associée au film.", e);
            }
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        getServletContext().getRequestDispatcher(path).forward(request, response);
    }
}
