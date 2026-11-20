package fr.turgot.servlet.employe;

import java.io.IOException;
import java.util.List;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Film;
import fr.turgot.dao.model.Utilisateur;
import fr.turgot.dto.TmdbMovie;
import fr.turgot.service.TmdbApiClient;
import fr.turgot.utils.CsrfProtection;
import fr.turgot.utils.PosterStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "TmdbCatalogImportServlet", urlPatterns = {
        "/employe/tmdb/catalogue",
        "/catalogueTmdb"
})
public class TmdbCatalogImportServlet extends HttpServlet {

    private static final String SUGGESTIONS_JSP = "/WEB-INF/Employer/TmdbSuggestions.jsp";
    private static final String REVIEW_JSP = "/WEB-INF/Employer/TmdbReview.jsp";

    private final FilmDAO filmDAO;
    private final TmdbApiClient tmdbApiClient;

    public TmdbCatalogImportServlet() {
        this(new FilmDAO(), new TmdbApiClient());
    }

    TmdbCatalogImportServlet(FilmDAO filmDAO, TmdbApiClient tmdbApiClient) {
        this.filmDAO = filmDAO;
        this.tmdbApiClient = tmdbApiClient;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur employee = getEmployee(request);
        if (employee == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CsrfProtection.ensureToken(request);

        String tmdbIdParameter = request.getParameter("tmdbId");
        try {
            if (tmdbIdParameter == null || tmdbIdParameter.isBlank()) {
                List<TmdbMovie> movies = tmdbApiClient.getNowPlayingMovies(employee.getLogin());
                request.setAttribute("movies", movies);
                forward(request, response, SUGGESTIONS_JSP);
                return;
            }

            int tmdbId = parseTmdbId(tmdbIdParameter);
            TmdbMovie movie = tmdbApiClient.getMovieDetails(tmdbId, employee.getLogin());
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
        Utilisateur employee = getEmployee(request);
        if (employee == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!CsrfProtection.isValid(request)) {
            getServletContext().log("Requête d'import TMDB refusée : jeton CSRF absent ou invalide.");
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
            TmdbMovie tmdbMovie = tmdbApiClient.getMovieDetails(tmdbId, employee.getLogin());
            if (filmDAO.getFilmByTmdbId(tmdbId) != null) {
                boolean cacheUpdated = updateTrailerCache(tmdbMovie, employee);
                redirectImportResult(request, response, "exists", cacheUpdated);
                return;
            }

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

            byte[] posterBytes = tmdbApiClient.downloadPoster(tmdbId, employee.getLogin());
            try {
                localPosterPath = PosterStorage.save(posterBytes);
            } catch (PosterStorage.InvalidImageException e) {
                request.setAttribute("error", e.getMessage());
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            } catch (PosterStorage.StorageConfigurationException e) {
                getServletContext().log("Le stockage persistant des affiches n'est pas configuré.", e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Le stockage des affiches n'est pas configuré.");
                return;
            } catch (IOException e) {
                getServletContext().log("Impossible d'enregistrer l'affiche TMDB localement.", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Impossible d'enregistrer l'affiche.");
                return;
            }

            Film film = new Film(title, director, year, genre, localPosterPath, duration);
            film.setTmdbId(tmdbId);
            if (filmDAO.create(film) == null) {
                Film concurrentFilm = filmDAO.getFilmByTmdbId(tmdbId);
                if (concurrentFilm != null) {
                    cleanupPoster(localPosterPath, request);
                    localPosterPath = null;
                    boolean cacheUpdated = updateTrailerCache(tmdbMovie, employee);
                    redirectImportResult(request, response, "exists", cacheUpdated);
                    return;
                }
                cleanupPoster(localPosterPath, request);
                localPosterPath = null;
                request.setAttribute("error", "Impossible d'enregistrer le film dans le catalogue.");
                setReviewAttributes(request, tmdbMovie);
                forward(request, response, REVIEW_JSP);
                return;
            }

            localPosterPath = null;
            boolean cacheUpdated = updateTrailerCache(tmdbMovie, employee);
            redirectImportResult(request, response, "created", cacheUpdated);
        } catch (InterruptedException e) {
            cleanupPoster(localPosterPath, request);
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (IOException e) {
            cleanupPoster(localPosterPath, request);
            getServletContext().log("Erreur de communication avec TMDB pendant l'import.", e);
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY,
                    "TMDB ou son serveur d'affiches est temporairement indisponible.");
        } catch (RuntimeException e) {
            cleanupPoster(localPosterPath, request);
            getServletContext().log("Erreur lors de l'import du film TMDB.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Utilisateur getEmployee(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            getServletContext().log("Requête du catalogue TMDB refusée : session absente.");
            return null;
        }
        Object sessionUser = session.getAttribute("usere");
        if (!(sessionUser instanceof Utilisateur user)
                || !"EMPLOYE".equalsIgnoreCase(user.getTypeUtilisateur())) {
            getServletContext().log("Requête du catalogue TMDB refusée : rôle employé requis.");
            return null;
        }
        return user;
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

    private void cleanupPoster(String relativePath, HttpServletRequest request) {
        if (relativePath != null) {
            deletePoster(relativePath, request);
        }
    }

    private void deletePoster(String relativePath, HttpServletRequest request) {
        try {
            PosterStorage.delete(relativePath, request.getServletContext());
        } catch (IOException e) {
            getServletContext().log("Impossible de supprimer l'affiche non associée au film.", e);
        }
    }

    private boolean updateTrailerCache(TmdbMovie movie, Utilisateur employee) {
        try {
            tmdbApiClient.cacheTrailerForMovie(
                    movie.getTmdbId(), movie.getTitle(), employee.getLogin());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getServletContext().log("La mise à jour du cache de bande-annonce a été interrompue.", e);
            return false;
        } catch (IOException e) {
            getServletContext().log("Le film est enregistré, mais son cache de bande-annonce n'a pas été mis à jour.", e);
            return false;
        }
    }

    private void redirectImportResult(
            HttpServletRequest request,
            HttpServletResponse response,
            String result,
            boolean cacheUpdated) throws IOException {
        String location = request.getContextPath() + "/listeFilms?import=" + result;
        if (!cacheUpdated) {
            location += "&cache=unavailable";
        }
        response.sendRedirect(location);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        getServletContext().getRequestDispatcher(path).forward(request, response);
    }
}
