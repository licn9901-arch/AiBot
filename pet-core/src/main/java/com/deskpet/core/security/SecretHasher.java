package com.deskpet.core.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public SecretHash hash(String secret) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = pbkdf2(secret, salt);
        return new SecretHash(encodeBase64(derived), encodeBase64(salt));
    }

    public boolean matches(String secret, String saltBase64, String hashBase64) {
        if (secret == null || saltBase64 == null || hashBase64 == null) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] expected = Base64.getDecoder().decode(hashBase64);
            byte[] actual = pbkdf2(secret, salt);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] pbkdf2(String secret, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to hash secret", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
