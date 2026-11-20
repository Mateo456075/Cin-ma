package fr.turgot.servlet.employe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jakarta.servlet.annotation.WebServlet;

class TmdbCatalogImportServletMappingTest {

    @Test
    void mapsDocumentedAndLegacyCatalogueRoutes() {
        WebServlet mapping = TmdbCatalogImportServlet.class.getAnnotation(WebServlet.class);

        assertNotNull(mapping);
        assertArrayEquals(
                new String[] {"/employe/tmdb/catalogue", "/catalogueTmdb"},
                mapping.urlPatterns());
    }
}
