package fr.turgot.servlet.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.turgot.dao.model.DispositionSieges;

public class CreatSalleServletTest {
    @Test
    public void allowsNonNegativeBalconCountForRectangle() {
        assertTrue(CreatSalleServlet.isBalconyCountAllowed(DispositionSieges.RECTANGLE, 2));
        assertTrue(CreatSalleServlet.isBalconyCountAllowed(DispositionSieges.RECTANGLE, 0));
    }

    @Test
    public void forbidsBalconsForImax() {
        assertTrue(CreatSalleServlet.isBalconyCountAllowed(DispositionSieges.IMAX, 0));
        assertFalse(CreatSalleServlet.isBalconyCountAllowed(DispositionSieges.IMAX, 1));
    }

    @Test
    public void rejectsNegativeBalconCount() {
        assertFalse(CreatSalleServlet.isBalconyCountAllowed(DispositionSieges.ARC_DE_CERCLE, -1));
    }
}
