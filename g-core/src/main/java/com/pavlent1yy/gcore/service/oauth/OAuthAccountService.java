package com.pavlent1yy.gcore.service.oauth;

import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.entity.UserOAuthAccount;
import com.pavlent1yy.gcore.enums.OAuthProvider;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final UserOAuthAccountRepository oauthAccountRepository;
    private final GitHubOAuthService githubOAuthService;
    private final UserRepository userRepository;

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
                email = oauthUser.getAttribute("email");
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
                .orElseGet(() -> createUser(
                        provider,
                        providerUserId,
                        email
                ));
    }

    private User createUser(
            OAuthProvider provider,
            String providerUserId,
            String email
    ) {
        User user = userRepository
                .findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setPasswordHash(null);
                    newUser.setRole(Role.STUDENT);
                    newUser.setEnabled(true);

                    return userRepository.save(newUser);
                });

        UserOAuthAccount account = new UserOAuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);

        oauthAccountRepository.save(account);

        return user;
    }
}
