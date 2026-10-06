package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidResetTokenException;
import com.pavlent1yy.gcore.entity.PasswordResetToken;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.repository.PasswordResetTokenRepository;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${core.password-reset-token-ttl:PT1H}")
    private Duration tokenTtl;

    @Value("${core.public-url}")
    private String publicUrl;

    @Value("${core.mail-from}")
    private String mailFrom;

    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            OffsetDateTime now = OffsetDateTime.now();
            boolean coolingDown = tokenRepository
                    .findTopByUser_IdOrderByCreatedAtDesc(user.getId())
                    .map(token -> token.getCreatedAt().isAfter(now.minus(REQUEST_COOLDOWN)))
                    .orElse(false);

            if (coolingDown) {
                return;
            }

            tokenRepository.deleteAllByUser_Id(user.getId());

            String rawToken = SecureTokens.generate();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(SecureTokens.sha256(rawToken));
            token.setCreatedAt(now);
            token.setExpiresAt(now.plus(tokenTtl));
            tokenRepository.save(token);

            try {
                sendMessage(user.getEmail(), rawToken);
            } catch (MessagingException | MailException e) {
                log.error("Could not send password reset mail: {}", e.getClass().getSimpleName());
            }
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository
                .findByTokenHashAndUsedAtIsNull(SecureTokens.sha256(rawToken))
                .orElseThrow(() -> new InvalidResetTokenException(
                        "Ссылка для сброса недействительна или уже использована"
                ));

        OffsetDateTime now = OffsetDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            throw new InvalidResetTokenException("Срок действия ссылки истёк. Запроси сброс ещё раз");
        }

        PasswordPolicy.validate(newPassword);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        user.setUpdatedAt(now);
        token.setUsedAt(now);

        userRepository.save(user);
        tokenRepository.save(token);
        refreshSessionRepository.deleteAllByUser_Id(user.getId());
    }

    private void sendMessage(String recipient, String rawToken) throws MessagingException {
        String resetUrl = publicUrl.replaceAll("/+$", "") + "/reset-password#token=" + rawToken;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_NO,
                StandardCharsets.UTF_8.name()
        );
        helper.setFrom(mailFrom);
        helper.setTo(recipient);
        helper.setSubject("Сброс пароля в Gradinator");
        helper.setText("""
                <div style="font-family:Arial,sans-serif;line-height:1.5;color:#172033">
                  <h2>Сброс пароля</h2>
                  <p>Кто-то (надеемся, ты) запросил сброс пароля для аккаунта Gradinator.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#315efb;color:#fff;padding:12px 18px;border-radius:8px;text-decoration:none">
                      Задать новый пароль
                    </a>
                  </p>
                  <p>Ссылка действует %d мин. и сработает один раз. После сброса все входы на других устройствах завершатся.</p>
                  <p style="color:#667085;font-size:13px">Если ты не запрашивал сброс, просто проигнорируй письмо — пароль не изменится.</p>
                </div>
                """.formatted(resetUrl, tokenTtl.toMinutes()), true);
        mailSender.send(message);
    }
}
