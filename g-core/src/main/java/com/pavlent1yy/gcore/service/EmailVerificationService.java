package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.EmailDeliveryException;
import com.pavlent1yy.gcore.customExceptions.InvalidVerificationTokenException;
import com.pavlent1yy.gcore.entity.EmailVerificationToken;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.EmailVerificationTokenRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${core.email-verification-token-ttl}")
    private Duration tokenTtl;

    @Value("${core.public-url}")
    private String publicUrl;

    @Value("${core.mail-from}")
    private String mailFrom;

    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteAllByUser_Id(user.getId());

        String token = generateToken();
        OffsetDateTime now = OffsetDateTime.now();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(hash(token));
        verificationToken.setCreatedAt(now);
        verificationToken.setExpiresAt(now.plus(tokenTtl));
        tokenRepository.save(verificationToken);

        sendMessage(user.getEmail(), token);
    }

    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEnabled())) {
                return;
            }

            OffsetDateTime resendAllowedAt = OffsetDateTime.now().minus(RESEND_COOLDOWN);
            boolean coolingDown = tokenRepository
                    .findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                    .map(token -> token.getCreatedAt().isAfter(resendAllowedAt))
                    .orElse(false);

            if (!coolingDown) {
                sendVerificationEmail(user);
            }
        });
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository
                .findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .orElseThrow(() -> new InvalidVerificationTokenException(
                        "Ссылка подтверждения недействительна или уже использована"
                ));

        OffsetDateTime now = OffsetDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            throw new InvalidVerificationTokenException(
                    "Срок действия ссылки истёк. Запросите новое письмо"
            );
        }

        User user = token.getUser();
        user.setEnabled(true);
        user.setUpdatedAt(now);
        token.setUsedAt(now);
        userRepository.save(user);
        tokenRepository.save(token);
    }

    private void sendMessage(String recipient, String token) {
        String baseUrl = publicUrl.replaceAll("/+$", "");
        String verificationUrl = baseUrl + "/verify-email#token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_NO,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(mailFrom);
            helper.setTo(recipient);
            helper.setSubject("Подтвердите почту в Gradinator");
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;line-height:1.5;color:#172033">
                      <h2>Подтверждение почты</h2>
                      <p>Нажмите кнопку, чтобы завершить регистрацию в Gradinator.</p>
                      <p style="margin:24px 0">
                        <a href="%s" style="background:#315efb;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none">
                          Подтвердить почту
                        </a>
                      </p>
                      <p>Ссылка действует %d ч.</p>
                      <p style="color:#667085;font-size:13px">Если вы не регистрировались, просто проигнорируйте письмо.</p>
                    </div>
                    """.formatted(verificationUrl, tokenTtl.toHours()), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Не удалось отправить письмо подтверждения", e);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
