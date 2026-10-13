package com.studenthub.backend.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verifiesPasswordWithItsSaltAndRejectsWrongPassword() {
        PasswordHasher.HashedPassword stored = hasher.hash("correct horse battery".toCharArray());

        assertTrue(hasher.matches("correct horse battery".toCharArray(), stored.salt(), stored.hash()));
        assertFalse(hasher.matches("incorrect password".toCharArray(), stored.salt(), stored.hash()));
    }
}
