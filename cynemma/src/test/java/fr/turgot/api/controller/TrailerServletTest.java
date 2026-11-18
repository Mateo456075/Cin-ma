package fr.turgot.api.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.Test;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TrailerServletTest {

    @Test
    public void keepsTrailerRouteMapping() {
        WebServlet mapping = TrailerServlet.class.getAnnotation(WebServlet.class);

        assertArrayEquals(new String[] {"/api/trailer"}, mapping.value());
    }

    @Test
    public void returnsBadRequestWhenFilmIsMissing() throws Exception {
        CapturedResponse response = new CapturedResponse();

        new TestableTrailerServlet().invokeGet(request(Map.of()), response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertTrue(response.body.toString().contains("\"erreur\":\"Paramètre 'film' manquant\""));
    }

    @Test
    public void returnsBadRequestWhenYearIsNotNumeric() throws Exception {
        CapturedResponse response = new CapturedResponse();

        new TestableTrailerServlet().invokeGet(
                request(Map.of("film", "Interstellar", "year", "invalid")),
                response.proxy());

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.status);
        assertTrue(response.body.toString().contains("\"erreur\":\"Paramètre 'year' invalide\""));
    }

    private HttpServletRequest request(Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getParameter".equals(method.getName())) {
                        return parameters.get(args[0]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class TestableTrailerServlet extends TrailerServlet {
        private void invokeGet(HttpServletRequest request, HttpServletResponse response)
                throws Exception {
            doGet(request, response);
        }
    }

    private static final class CapturedResponse {
        private final StringWriter body = new StringWriter();
        private int status = HttpServletResponse.SC_OK;

        private HttpServletResponse proxy() {
            PrintWriter writer = new PrintWriter(body);
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setStatus" -> status = (int) args[0];
                            case "getWriter" -> {
                                return writer;
                            }
                            case "setContentType", "setCharacterEncoding" -> {
                                return null;
                            }
                            default -> throw new UnsupportedOperationException(method.getName());
                        }
                        return null;
                    });
        }
    }
}
