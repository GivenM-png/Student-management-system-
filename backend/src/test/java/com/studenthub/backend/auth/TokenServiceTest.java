package com.studenthub.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TokenServiceTest {
    @Test
    void tokenRetainsAccountRoleUntilItIsRevoked() {
        TokenService tokens = new TokenService();
        String token = tokens.issue("student@example.test", "STUDENT");

        assertEquals("student@example.test", tokens.emailForValidToken(token));
        assertEquals("STUDENT", tokens.roleForValidToken(token));

        tokens.revoke(token);

        assertNull(tokens.emailForValidToken(token));
        assertNull(tokens.roleForValidToken(token));
    }
}
