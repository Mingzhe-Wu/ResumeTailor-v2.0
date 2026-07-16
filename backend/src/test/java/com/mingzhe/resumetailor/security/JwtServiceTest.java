package com.mingzhe.resumetailor.security;

import com.mingzhe.resumetailor.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "ci-only-jwt-secret-that-is-long-enough-for-hs256-tests";

    @Test
    void generatesAndValidatesTokenForUser() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000);
        User user = new User();
        user.setId(42L);
        user.setEmail("developer@example.com");

        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(user.getEmail(), jwtService.extractEmail(token));
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        User user = new User();
        user.setId(42L);
        user.setEmail("developer@example.com");

        JwtService validJwtService = new JwtService(TEST_SECRET, 60_000);
        String token = validJwtService.generateToken(user);
        assertFalse(validJwtService.isTokenValid(token + "tampered"));

        JwtService expiredJwtService = new JwtService(TEST_SECRET, -1_000);
        assertFalse(expiredJwtService.isTokenValid(expiredJwtService.generateToken(user)));
    }
}
