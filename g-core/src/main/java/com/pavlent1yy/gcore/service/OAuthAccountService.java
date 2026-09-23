package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.entity.UserOAuthAccount;
import com.pavlent1yy.gcore.enums.OAuthProvider;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.repository.UserOAuthAccountRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final UserOAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(OAuth2User oauthUser) {
        String providerUserId = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");

        if (providerUserId == null || email == null) {
            throw new IllegalStateException(
                    "Google не предоставил обязательные данные пользователя"
            );
        }

        return oauthAccountRepository
                .findByProviderAndProviderUserId(
                        OAuthProvider.GOOGLE,
                        providerUserId
                )
                .map(UserOAuthAccount::getUser)
                .orElseGet(() -> createUser(
                        providerUserId,
                        email
                ));
    }

    private User createUser(String providerUserId, String email) {
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
        account.setProvider(OAuthProvider.GOOGLE);
        account.setProviderUserId(providerUserId);

        oauthAccountRepository.save(account);

        return user;
    }
}
