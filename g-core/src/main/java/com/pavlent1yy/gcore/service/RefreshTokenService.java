package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidRefreshTokenException;
import com.pavlent1yy.gcore.entity.RefreshSession;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshSessionRepository refreshSessionRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    public String create(User user) {
        String token = generateToken();

        RefreshSession session = RefreshSession.builder()
                .user(user)
                .refreshTokenHash(hash(token))
                .createdAt(OffsetDateTime.now())
                .expiresAt(
                        OffsetDateTime.now()
                                .plus(Duration.ofMillis(refreshExpiration))
                )
                .build();

        refreshSessionRepository.save(session);

        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Transactional
    public void revoke(String token) {
        RefreshSession session = refreshSessionRepository
                .findByRefreshTokenHash(hash(token))
                .orElseThrow(() ->  new InvalidRefreshTokenException("Invalid refresh token"));

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(OffsetDateTime.now());
            refreshSessionRepository.save(session);
        }
    }

    @Transactional
    public String rotate(RefreshSession oldSession) {
        oldSession.setRevokedAt(OffsetDateTime.now());
        refreshSessionRepository.save(oldSession);

        return create(oldSession.getUser());
    }
}
