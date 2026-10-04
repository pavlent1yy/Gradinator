package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.EmailDeliveryException;
import com.pavlent1yy.gcore.customExceptions.InvalidVerificationTokenException;
import com.pavlent1yy.gcore.entity.EmailVerificationToken;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.EmailVerificationTokenRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    private final EmailVerificationTokenRepository tokenRepository = mock(EmailVerificationTokenRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(tokenRepository, userRepository, mailSender);
        ReflectionTestUtils.setField(service, "tokenTtl", Duration.ofHours(24));
        ReflectionTestUtils.setField(service, "publicUrl", "https://gradinator.test///");
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@gradinator.test");
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private static User user(Long id, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setEnabled(enabled);
        return user;
    }

    private static String sha256Hex(String token) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private EmailVerificationToken savedToken() {
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendsLinkAndStoresOnlyTokenHash() throws Exception {
        User user = user(1L, "a@mail.ru", false);

        service.sendVerificationEmail(user);

        verify(tokenRepository).deleteAllByUser_Id(1L);
        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo("Подтвердите почту в Gradinator");
        assertThat(message.getFrom()).containsExactly(new InternetAddress("noreply@gradinator.test"));
        assertThat(message.getAllRecipients()).containsExactly(new InternetAddress("a@mail.ru"));

        String html = (String) message.getContent();
        Matcher link = Pattern.compile("https://gradinator\\.test/verify-email#token=([A-Za-z0-9_-]+)").matcher(html);
        assertThat(link.find()).isTrue();
        assertThat(html).contains("Ссылка действует 24 ч.");

        EmailVerificationToken token = savedToken();
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo(sha256Hex(link.group(1)));
        assertThat(token.getExpiresAt()).isCloseTo(OffsetDateTime.now().plusHours(24), within(Duration.ofMinutes(1)));
    }

    @Test
    void mailFailureBecomesEmailDeliveryException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.sendVerificationEmail(user(1L, "a@mail.ru", false)))
                .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void resendIgnoresUnknownEmail() {
        when(userRepository.findByEmail("nobody@mail.ru")).thenReturn(Optional.empty());

        service.resend(" Nobody@Mail.ru ");

        verifyNoInteractions(mailSender);
    }

    @Test
    void resendIgnoresAlreadyEnabledUser() {
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user(1L, "a@mail.ru", true)));

        service.resend("a@mail.ru");

        verifyNoInteractions(mailSender, tokenRepository);
    }

    @Test
    void resendRespectsCooldown() {
        EmailVerificationToken recent = new EmailVerificationToken();
        recent.setCreatedAt(OffsetDateTime.now().minusSeconds(10));
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user(1L, "a@mail.ru", false)));
        when(tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(recent));

        service.resend("a@mail.ru");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void resendSendsAfterCooldown() {
        EmailVerificationToken old = new EmailVerificationToken();
        old.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user(1L, "a@mail.ru", false)));
        when(tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(old));

        service.resend("A@mail.ru");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void resendSendsWhenNoTokenYet() {
        when(userRepository.findByEmail("a@mail.ru")).thenReturn(Optional.of(user(1L, "a@mail.ru", false)));
        when(tokenRepository.findTopByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        service.resend("a@mail.ru");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void verifyEnablesUserAndMarksTokenUsed() throws Exception {
        User user = user(1L, "a@mail.ru", false);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(sha256Hex("raw"))).thenReturn(Optional.of(token));

        service.verify("raw");

        assertThat(user.getEnabled()).isTrue();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyRejectsUnknownOrUsedToken() {
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("raw"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining("недействительна");
    }

    @Test
    void verifyRejectsExpiredToken() throws Exception {
        User user = user(1L, "a@mail.ru", false);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(sha256Hex("raw"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify("raw"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining("истёк");
        assertThat(user.getEnabled()).isFalse();
        verify(userRepository, never()).save(any());
    }
}
