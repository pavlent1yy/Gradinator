package com.pavlent1yy.gcore.service.oauth;

import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.entity.UserOAuthAccount;
import com.pavlent1yy.gcore.enums.OAuthProvider;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.RefreshSessionRepository;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.service.EmailNormalizer;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final UserOAuthAccountRepository oauthAccountRepository;
    private final GitHubOAuthService githubOAuthService;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;

    @Value("${core.email-verification:false}")
    private boolean emailVerificationEnabled;

    @Transactional
    public User getOrCreateUser(OAuth2User oauthUser, String registrationId, String accessToken) {
        OAuthProvider provider = OAuthProvider.valueOf(
                registrationId.toUpperCase()
        );

        String providerUserId;
        String email;

        switch (provider) {
            case GOOGLE -> {
                providerUserId = oauthUser.getAttribute("sub");
                email = Boolean.TRUE.equals(oauthUser.getAttribute("email_verified"))
                        ? oauthUser.getAttribute("email")
                        : null;
            }

            case GITHUB -> {
                providerUserId = Objects.toString(
                        oauthUser.getAttribute("id"),
                        null
                );

                email = githubOAuthService.getEmail(accessToken);
            }

            case VK -> {
                providerUserId = Objects.toString(
                        oauthUser.getAttribute("user_id"),
                        null
                );
                email = oauthUser.getAttribute("email");
            }

            case YANDEX -> {
                providerUserId = Objects.toString(
                        oauthUser.getAttribute("id"),
                        null
                );
                email = oauthUser.getAttribute("default_email");
            }

            default -> throw new IllegalStateException(
                    "Неподдерживаемый OAuth provider: " + provider
            );
        }

        if (providerUserId == null || email == null || "null".equals(email)) {
            throw new IllegalStateException(
                    provider + " не предоставил обязательные данные пользователя"
            );
        }

        return oauthAccountRepository
                .findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                )
                .map(UserOAuthAccount::getUser)
                .orElseGet(() -> createOAuthUser(
                        provider,
                        providerUserId,
                        email
                ));
    }

    private User createOAuthUser(
            OAuthProvider provider,
            String providerUserId,
            String email
    ) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        User user = existing
                .orElseGet(() -> {
                    User newUser = new User();

                    newUser.setEmail(normalizedEmail);
                    newUser.setPasswordHash(null);
                    newUser.setRole(Role.STUDENT);
                    newUser.setEnabled(true);
                    newUser.setRegisteredAt(OffsetDateTime.now());

                    return userRepository.save(newUser);
                });

        if (existing.isPresent()) {
            secureExistingAccount(user);
        }

        if (!oauthAccountRepository
                .existsByProviderAndProviderUserId(provider, providerUserId)) {

            UserOAuthAccount account = new UserOAuthAccount();
            account.setUser(user);
            account.setProvider(provider);
            account.setProviderUserId(providerUserId);

            oauthAccountRepository.save(account);
        }

        return user;
    }

    private void secureExistingAccount(User user) {
        boolean emailProvenByOwner = emailVerificationEnabled && Boolean.TRUE.equals(user.getEnabled());
        boolean firstExternalLogin = !oauthAccountRepository.existsByUser_Id(user.getId());

        if (user.getPasswordHash() != null && !emailProvenByOwner && firstExternalLogin) {
            log.warn("OAuth login replaced an unverified password and revoked sessions");
            user.setPasswordHash(null);
            refreshSessionRepository.deleteAllByUser_Id(user.getId());
        }

        user.setEnabled(true);
        userRepository.save(user);
    }
}
