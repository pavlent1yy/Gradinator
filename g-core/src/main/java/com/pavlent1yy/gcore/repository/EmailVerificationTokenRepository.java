package com.pavlent1yy.gcore.repository;

import com.pavlent1yy.gcore.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    Optional<EmailVerificationToken> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteAllByUser_Id(Long userId);
}
