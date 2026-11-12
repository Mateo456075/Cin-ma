package fr.turgot.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class CsrfProtection {
    private static final String SESSION_ATTRIBUTE = CsrfProtection.class.getName() + ".token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfProtection() {
    }

    public static String ensureToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        synchronized (session) {
            String token = (String) session.getAttribute(SESSION_ATTRIBUTE);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(SESSION_ATTRIBUTE, token);
            }
            request.setAttribute("csrfToken", token);
            return token;
        }
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_ATTRIBUTE);
        String supplied = request.getParameter("csrfToken");
        if (!(expected instanceof String) || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                ((String) expected).getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
