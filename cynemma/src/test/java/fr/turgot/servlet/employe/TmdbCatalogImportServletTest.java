package fr.turgot.servlet.employe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Client;
import fr.turgot.dao.model.Employe;
import fr.turgot.dao.model.Film;
import fr.turgot.dto.TmdbMovie;
import fr.turgot.service.TmdbApiClient;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class TmdbCatalogImportServletTest {
    private static final String CSRF_TOKEN_ATTRIBUTE = CsrfProtection.class.getName() + ".token";
    private static final int TMDB_ID = 1263337;

    @TempDir
    Path temporaryDirectory;

    private Map<String, Object> sessionAttributes;
    private Map<String, String> parameters;
    private HttpSession session;
    private ServletContext servletContext;
    private ServletTestSupport.ResponseState responseState;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private List<String> events;
    private StubFilmDAO filmDAO;
    private StubTmdbApiClient tmdbApiClient;
    private TmdbCatalogImportServlet servlet;
    private String previousImageDirectory;

    @BeforeEach
    void setUp() throws Exception {
        previousImageDirectory = System.getProperty("CYNEMMA_IMAGE_DIR");
        System.setProperty("CYNEMMA_IMAGE_DIR", temporaryDirectory.toString());

        sessionAttributes = new HashMap<>();
        parameters = new HashMap<>();
        parameters.put("tmdbId", Integer.toString(TMDB_ID));
        parameters.put("title", "Film TMDB");
        parameters.put("director", "Réalisateur");
        parameters.put("year", "2024");
        parameters.put("genre", "Drame");
        parameters.put("duration", "120");
        parameters.put("csrfToken", "valid-csrf-token");
        sessionAttributes.put(CSRF_TOKEN_ATTRIBUTE, "valid-csrf-token");

        session = ServletTestSupport.session(sessionAttributes);
        servletContext = ServletTestSupport.servletContext();
        responseState = new ServletTestSupport.ResponseState();
        request = ServletTestSupport.request(parameters, session, servletContext, "/cinema");
        response = ServletTestSupport.response(responseState);

        events = new ArrayList<>();
        filmDAO = new StubFilmDAO(events);
        tmdbApiClient = new StubTmdbApiClient(events);
        tmdbApiClient.movie = movie();
        servlet = new TmdbCatalogImportServlet(filmDAO, tmdbApiClient);
        servlet.init(ServletTestSupport.servletConfig(servletContext));
    }

    @AfterEach
    void restoreImageDirectory() {
        if (previousImageDirectory == null) {
            System.clearProperty("CYNEMMA_IMAGE_DIR");
        } else {
            System.setProperty("CYNEMMA_IMAGE_DIR", previousImageDirectory);
        }
    }

    @Test
    void refusesImportFromNonEmployee() throws Exception {
        sessionAttributes.put("usere", new Client());

        servlet.doPost(request, response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseState.errorStatus);
        assertEquals(0, tmdbApiClient.detailsCalls);
        assertEquals(0, filmDAO.createCalls);
    }

    @Test
    void refusesImportWhenCsrfTokenDoesNotMatch() throws Exception {
        sessionAttributes.put("usere", employee());
        parameters.put("csrfToken", "incorrect-token");

        servlet.doPost(request, response);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, responseState.errorStatus);
        assertEquals(0, tmdbApiClient.detailsCalls);
        assertEquals(0, filmDAO.createCalls);
    }

    @Test
    void doesNotCreateFilmAgainWhenTmdbIdAlreadyExists() throws Exception {
        sessionAttributes.put("usere", employee());
        filmDAO.existingFilm = new Film();
        tmdbApiClient.posterBytes = validPng();

        servlet.doPost(request, response);

        assertEquals("/cinema/listeFilms?import=exists", responseState.redirect);
        assertEquals(0, tmdbApiClient.posterCalls);
        assertEquals(0, filmDAO.createCalls);
        assertEquals(List.of("tmdb.details", "film.find", "tmdb.cache"), events);
    }

    @Test
    void createsFilmAndStoresPosterEvenIfTrailerCacheUpdateFails() throws Exception {
        sessionAttributes.put("usere", employee());
        tmdbApiClient.posterBytes = validPng();
        tmdbApiClient.cacheFailure = new IOException("Cache indisponible");

        servlet.doPost(request, response);

        assertEquals("/cinema/listeFilms?import=created&cache=unavailable", responseState.redirect);
        assertEquals(List.of("tmdb.details", "film.find", "tmdb.poster", "film.create", "tmdb.cache"),
                events);
        assertNotNull(filmDAO.createdFilm);
        assertEquals(TMDB_ID, filmDAO.createdFilm.getTmdbId());
        assertTrue(filmDAO.createdFilm.getAffiche().startsWith("image/"));
        assertTrue(Files.isRegularFile(
                temporaryDirectory.resolve(Path.of(filmDAO.createdFilm.getAffiche()).getFileName())));
        assertNull(responseState.errorStatus);
    }

    @Test
    void cleansUpPosterIfFilmCannotBeCreated() throws Exception {
        sessionAttributes.put("usere", employee());
        tmdbApiClient.posterBytes = validPng();
        filmDAO.rejectCreate = true;

        servlet.doPost(request, response);

        assertNull(responseState.redirect);
        assertNotNull(request.getAttribute("error"));
        assertTrue(events.contains("film.create"));
        try (var storedFiles = Files.list(temporaryDirectory)) {
            assertFalse(storedFiles.findAny().isPresent());
        }
    }

    private Employe employee() {
        Employe employee = new Employe();
        employee.setLogin("employee");
        return employee;
    }

    private TmdbMovie movie() {
        return new TmdbMovie(
                TMDB_ID, "Film TMDB", "Synopsis", "2024-01-01", "/poster.jpg",
                120, "Réalisateur", "Drame");
    }

    private byte[] validPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private static final class StubFilmDAO extends FilmDAO {
        private final List<String> events;
        private Film existingFilm;
        private Film createdFilm;
        private boolean rejectCreate;
        private int createCalls;

        private StubFilmDAO(List<String> events) {
            this.events = events;
        }

        @Override
        public Film getFilmByTmdbId(int tmdbId) {
            events.add("film.find");
            return existingFilm;
        }

        @Override
        public Film create(Film film) {
            events.add("film.create");
            createCalls++;
            createdFilm = film;
            return rejectCreate ? null : film;
        }
    }

    private static final class StubTmdbApiClient extends TmdbApiClient {
        private final List<String> events;
        private TmdbMovie movie;
        private byte[] posterBytes;
        private IOException cacheFailure;
        private int detailsCalls;
        private int posterCalls;

        private StubTmdbApiClient(List<String> events) {
            this.events = events;
        }

        @Override
        public TmdbMovie getMovieDetails(int tmdbId, String employeeLogin) {
            events.add("tmdb.details");
            detailsCalls++;
            return movie;
        }

        @Override
        public byte[] downloadPoster(int tmdbId, String employeeLogin) {
            events.add("tmdb.poster");
            posterCalls++;
            return posterBytes;
        }

        @Override
        public void cacheTrailerForMovie(int tmdbId, String title, String employeeLogin)
                throws IOException {
            events.add("tmdb.cache");
            if (cacheFailure != null) {
                throw cacheFailure;
            }
        }
    }
}
