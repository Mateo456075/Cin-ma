package fr.turgot.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PageServletTest {
    @Test
    public void allowsKnownPublicPages() {
        assertTrue(PageServlet.isAllowedPublicPage("Authentification"));
        assertTrue(PageServlet.isAllowedPublicPage("visiteur/visiteur"));
    }

    @Test
    public void rejectsTraversalAndProtectedPages() {
        assertFalse(PageServlet.isAllowedPublicPage("../admin/admin"));
        assertFalse(PageServlet.isAllowedPublicPage("admin/admin"));
        assertFalse(PageServlet.isAllowedPublicPage("includes/header"));
        assertFalse(PageServlet.isAllowedPublicPage(null));
    }
}
