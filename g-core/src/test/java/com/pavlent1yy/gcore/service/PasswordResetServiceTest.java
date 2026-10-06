package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidResetTokenException;
import com.pavlent1yy.gcore.entity.PasswordResetToken;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.PasswordResetTokenRepository;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tokenTtl", Duration.ofHours(1));
        ReflectionTestUtils.setField(service, "publicUrl", "https://site.test/");
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@site.test");

        user = new User();
        user.setId(5L);
        user.setEmail("user@mail.ru");
        user.setEnabled(false);
        when(userRepository.findByEmail("user@mail.ru")).thenReturn(Optional.of(user));
        when(tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(5L)).thenReturn(Optional.empty());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    private static PasswordResetToken token(User owner, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(owner);
        token.setCreatedAt(createdAt);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void unknownEmailDoesNothingAndRevealsNothing() {
        service.requestReset("nobody@mail.ru");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(mailSender);
    }

    @Test
    void requestStoresOnlyHashAndMailsLinkWithRawToken() throws Exception {
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);

        service.requestReset("  USER@mail.ru ");

        verify(tokenRepository).deleteAllByUser_Id(5L);
        verify(tokenRepository).save(saved.capture());
        verify(mailSender).send(sent.capture());

        String body = (String) sent.getValue().getContent();
        String rawToken = body.replaceAll("(?s).*reset-password#token=([A-Za-z0-9_-]+).*", "$1");
        assertThat(body).contains("https://site.test/reset-password#token=");
        assertThat(saved.getValue().getTokenHash()).isEqualTo(SecureTokens.sha256(rawToken)).isNotEqualTo(rawToken);
        assertThat(saved.getValue().getExpiresAt()).isAfter(OffsetDateTime.now().plusMinutes(59));
    }

    @Test
    void repeatedRequestWithinCooldownIsIgnored() {
        when(tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(5L))
                .thenReturn(Optional.of(token(user, OffsetDateTime.now().minusSeconds(10), OffsetDateTime.now().plusHours(1))));

        service.requestReset("user@mail.ru");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(mailSender);
    }

    @Test
    void mailFailureDoesNotLeakAccountExistence() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        service.requestReset("user@mail.ru");

        verify(tokenRepository).save(any());
    }

    @Test
    void resetSetsPasswordEnablesUserAndKillsSessions() {
        PasswordResetToken token = token(user, OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(SecureTokens.sha256("raw"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        service.resetPassword("raw", "new-pass");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getEnabled()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(refreshSessionRepository).deleteAllByUser_Id(5L);
    }

    @Test
    void unknownUsedOrExpiredTokenIsRejected() {
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("raw", "x")).isInstanceOf(InvalidResetTokenException.class);

        PasswordResetToken expired = token(user, OffsetDateTime.now().minusHours(2), OffsetDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(SecureTokens.sha256("old"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resetPassword("old", "x"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessageContaining("истёк");

        verifyNoInteractions(passwordEncoder, refreshSessionRepository);
        assertThat(user.getPasswordHash()).isNull();
    }
}
