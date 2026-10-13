package com.studenthub.backend.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 43_200;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

    public String issue(String email, String role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(email, role, Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS)));
        return token;
    }

    public boolean isValid(String token) {
        return emailForValidToken(token) != null;
    }

    public String emailForValidToken(String token) {
        Session session = validSession(token);
        return session == null ? null : session.email();
    }

    public String roleForValidToken(String token) {
        Session session = validSession(token);
        return session == null ? null : session.role();
    }

    private Session validSession(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token, session);
            return null;
        }
        return session;
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    private record Session(String email, String role, Instant expiresAt) {}
}
