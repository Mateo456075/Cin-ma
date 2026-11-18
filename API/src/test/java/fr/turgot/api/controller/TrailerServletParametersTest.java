package fr.turgot.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TrailerServletParametersTest {
    @Test
    void acceptsMissingBlankAndValidYears() {
        assertNull(TrailerServlet.parseOptionalYear(null));
        assertNull(TrailerServlet.parseOptionalYear(" "));
        assertEquals(2014, TrailerServlet.parseOptionalYear("2014"));
    }

    @Test
    void rejectsNonNumericAndOutOfRangeYears() {
        assertThrows(IllegalArgumentException.class,
                () -> TrailerServlet.parseOptionalYear("deux-mille"));
        assertThrows(IllegalArgumentException.class,
                () -> TrailerServlet.parseOptionalYear("1887"));
        assertThrows(IllegalArgumentException.class,
                () -> TrailerServlet.parseOptionalYear("10000"));
    }
}
