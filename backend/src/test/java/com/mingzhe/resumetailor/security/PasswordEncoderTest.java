package com.mingzhe.resumetailor.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    @Test
    void bcryptHashesAndMatchesPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "correct horse battery staple";

        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
        assertFalse(encoder.matches("wrong password", encodedPassword));
    }
}
