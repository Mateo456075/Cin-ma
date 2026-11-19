package fr.turgot.servlet.employe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.turgot.dao.FilmDAO;
import fr.turgot.dao.model.Employe;
import fr.turgot.dao.model.Film;
import fr.turgot.utils.CsrfProtection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class EditFilmServletTest {
    @Test
    void preservesTmdbIdWhenUpdatingFilm() throws Exception {
        Film film = new Film("Avant", "Réalisateur", 2020, "Drame", "image/affiche.png", 100);
        film.setId(7);
        film.setTmdbId(1263337);
        CapturingFilmDAO filmDAO = new CapturingFilmDAO(film);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("usere", new Employe());
        attributes.put(CsrfProtection.class.getName() + ".token", "csrf-token");
        HttpSession session = ServletTestSupport.session(attributes);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("csrfToken", "csrf-token");
        parameters.put("id", "7");
        parameters.put("titre", "Après");
        parameters.put("realisateur", "Nouveau réalisateur");
        parameters.put("annee", "2025");
        parameters.put("genre", "Science-fiction");
        parameters.put("temps", "110");

        ServletTestSupport.ResponseState responseState = new ServletTestSupport.ResponseState();
        HttpServletRequest request = ServletTestSupport.request(
                parameters, session, ServletTestSupport.servletContext(), "/cinema");
        HttpServletResponse response = ServletTestSupport.response(responseState);

        new EditFilmServlet(filmDAO).doPost(request, response);

        assertNotNull(filmDAO.updatedFilm);
        assertEquals(1263337, filmDAO.updatedFilm.getTmdbId());
        assertEquals("Après", filmDAO.updatedFilm.getTitre());
        assertEquals("/cinema/listeFilms", responseState.redirect);
    }

    private static final class CapturingFilmDAO extends FilmDAO {
        private final Film film;
        private Film updatedFilm;

        private CapturingFilmDAO(Film film) {
            this.film = film;
        }

        @Override
        public Film getFilmById(int id) {
            return film;
        }

        @Override
        public boolean update(Film film) {
            updatedFilm = film;
            return true;
        }
    }
}
