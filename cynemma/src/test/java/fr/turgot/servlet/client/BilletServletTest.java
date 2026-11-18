package fr.turgot.servlet.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BilletServletTest {
    @Test
    public void parsesPositiveIntegerParameters() {
        assertEquals(4, BilletServlet.parsePositiveInteger(" 4 "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingIntegerParameter() {
        BilletServlet.parsePositiveInteger(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonNumericIntegerParameter() {
        BilletServlet.parsePositiveInteger("two");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroOrNegativeIntegerParameter() {
        BilletServlet.parsePositiveInteger("-1");
    }
}
