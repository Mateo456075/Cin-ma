package fr.turgot.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class InternalTmdbServletTest {
    private final InternalTmdbServlet servlet = new InternalTmdbServlet();

    @Test
    public void acceptsApplicationJsonWithUtf8OrNoCharset() {
        assertTrue(servlet.isUtf8Json("application/json"));
        assertTrue(servlet.isUtf8Json("application/json; charset=UTF-8"));
    }

    @Test
    public void rejectsOtherMediaTypesAndCharsets() {
        assertFalse(servlet.isUtf8Json("application/jsonp"));
        assertFalse(servlet.isUtf8Json("text/json"));
        assertFalse(servlet.isUtf8Json("application/json; charset=ISO-8859-1"));
    }

    @Test
    public void parsesUtf8JsonBody() {
        var result = servlet.parseJson("{\"title\":\"Été\"}".getBytes(StandardCharsets.UTF_8));

        assertEquals("Été", result.getString("title"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedJsonBody() {
        servlet.parseJson("{".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidUtf8Body() {
        servlet.parseJson(new byte[] {(byte) 0xC3, (byte) 0x28});
    }
}
