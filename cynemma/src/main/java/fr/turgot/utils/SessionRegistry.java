package fr.turgot.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public final class SessionRegistry implements HttpSessionListener {
    private static final ConcurrentMap<String, Set<HttpSession>> SESSIONS_BY_LOGIN = new ConcurrentHashMap<>();
    private static final ConcurrentMap<HttpSession, String> LOGIN_BY_SESSION = new ConcurrentHashMap<>();

    public static void register(HttpSession session, String login) {
        unregister(session);
        LOGIN_BY_SESSION.put(session, login);
        SESSIONS_BY_LOGIN.computeIfAbsent(login, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public static void invalidateSessions(String login) {
        Set<HttpSession> sessions = SESSIONS_BY_LOGIN.remove(login);
        if (sessions == null) {
            return;
        }
        for (HttpSession session : sessions) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // The session has already expired or been invalidated.
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        unregister(event.getSession());
    }

    private static void unregister(HttpSession session) {
        String login = LOGIN_BY_SESSION.remove(session);
        if (login == null) {
            return;
        }
        Set<HttpSession> sessions = SESSIONS_BY_LOGIN.get(login);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                SESSIONS_BY_LOGIN.remove(login, sessions);
            }
        }
    }
}
