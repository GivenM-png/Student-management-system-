package com.studenthub.backend.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    public HashedPassword hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return new HashedPassword(HexFormat.of().formatHex(salt),
                HexFormat.of().formatHex(derive(password, salt)));
    }

    public boolean matches(char[] password, String saltHex, String expectedHex) {
        try {
            byte[] salt = HexFormat.of().parseHex(saltHex);
            byte[] expected = HexFormat.of().parseHex(expectedHex);
            if (salt.length != SALT_BYTES || expected.length != HASH_BYTES) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt));
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public record HashedPassword(String salt, String hash) {}
}
