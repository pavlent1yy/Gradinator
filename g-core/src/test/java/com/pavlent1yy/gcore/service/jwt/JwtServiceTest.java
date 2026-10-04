package com.pavlent1yy.gcore.service.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaA==";
    private static final String OTHER_SECRET = java.util.Base64.getEncoder()
            .encodeToString("another-test-secret-that-is-long-enough-123".getBytes());

    private JwtService jwtService;

    private static JwtService service(String secret, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expiration", expiration);
        return service;
    }

    private static UserDetails user(String email) {
        return new User(email, "hash", List.of());
    }

    @BeforeEach
    void setUp() {
        jwtService = service(SECRET, 60_000);
    }

    @Test
    void tokenCarriesUsername() {
        String token = jwtService.generateToken(user("user@mail.ru"));

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@mail.ru");
        assertThat(jwtService.isTokenValid(token, user("user@mail.ru"))).isTrue();
    }

    @Test
    void tokenIsInvalidForAnotherUser() {
        String token = jwtService.generateToken(user("user@mail.ru"));

        assertThat(jwtService.isTokenValid(token, user("other@mail.ru"))).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = service(SECRET, -1_000).generateToken(user("user@mail.ru"));

        assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = service(OTHER_SECRET, 60_000).generateToken(user("user@mail.ru"));

        assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user("user@mail.ru"));
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];

        assertThatThrownBy(() -> jwtService.extractUsername(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}
