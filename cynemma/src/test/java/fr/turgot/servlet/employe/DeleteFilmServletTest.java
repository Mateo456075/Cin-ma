package fr.turgot.servlet.employe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeleteFilmServletTest {
    @Test
    public void onlyAllowsPathsInsideImageDirectory() {
        assertTrue(DeleteFilmServlet.isSafePosterPath("image/cover.png"));
        assertFalse(DeleteFilmServlet.isSafePosterPath("../cover.png"));
        assertFalse(DeleteFilmServlet.isSafePosterPath("image/../../WEB-INF/web.xml"));
        assertFalse(DeleteFilmServlet.isSafePosterPath("image"));
        assertFalse(DeleteFilmServlet.isSafePosterPath(null));
        assertFalse(DeleteFilmServlet.isSafePosterPath("image/" + (char) 0));
    }
}
