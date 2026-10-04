package com.pavlent1yy.gcore.service.jwt;

import com.pavlent1yy.gcore.entity.RefreshSession;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtRefreshTokenServiceTest {

    private final RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
    private JwtRefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new JwtRefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshExpiration", Duration.ofDays(30).toMillis());
    }

    private static String hash(String token) throws Exception {
        return Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private RefreshSession savedSession() {
        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createStoresOnlyHashOfToken() throws Exception {
        User user = new User();

        String token = service.create(user);

        RefreshSession session = savedSession();
        assertThat(token).matches("[A-Za-z0-9_-]{86}");
        assertThat(session.getRefreshTokenHash()).isEqualTo(hash(token)).isNotEqualTo(token);
        assertThat(session.getUser()).isSameAs(user);
        assertThat(session.getRevokedAt()).isNull();
        assertThat(session.getExpiresAt())
                .isCloseTo(OffsetDateTime.now().plusDays(30), within(Duration.ofMinutes(1)));
    }

    @Test
    void tokensAreUnique() {
        assertThat(service.create(new User())).isNotEqualTo(service.create(new User()));
    }

    @Test
    void revokeMarksActiveSession() throws Exception {
        RefreshSession session = RefreshSession.builder().build();
        when(repository.findByRefreshTokenHash(hash("token"))).thenReturn(Optional.of(session));

        service.revoke("token");

        assertThat(session.getRevokedAt()).isNotNull();
        verify(repository).save(session);
    }

    @Test
    void revokeKeepsFirstRevocationTime() throws Exception {
        OffsetDateTime revokedAt = OffsetDateTime.now().minusDays(1);
        RefreshSession session = RefreshSession.builder().revokedAt(revokedAt).build();
        when(repository.findByRefreshTokenHash(hash("token"))).thenReturn(Optional.of(session));

        service.revoke("token");

        assertThat(session.getRevokedAt()).isEqualTo(revokedAt);
        verify(repository, never()).save(any());
    }

    @Test
    void revokeOfUnknownTokenDoesNothing() {
        when(repository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());

        service.revoke("unknown");

        verify(repository, never()).save(any());
    }

    @Test
    void rotateRevokesOldAndIssuesNewForSameUser() {
        User user = new User();
        RefreshSession old = RefreshSession.builder().user(user).build();

        String newToken = service.rotate(old);

        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(newToken).isNotBlank();
        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(old);
        assertThat(captor.getAllValues().get(1).getUser()).isSameAs(user);
        assertThat(captor.getAllValues().get(1).getRevokedAt()).isNull();
    }
}
