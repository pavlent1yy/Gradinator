package com.pavlent1yy.gcore.repository;

import com.pavlent1yy.gcore.entity.UserOAuthAccount;
import com.pavlent1yy.gcore.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOAuthAccountRepository
        extends JpaRepository<UserOAuthAccount, Long> {

    Optional<UserOAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );
}
