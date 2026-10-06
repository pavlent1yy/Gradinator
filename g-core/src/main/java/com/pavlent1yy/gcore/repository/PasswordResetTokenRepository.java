package com.pavlent1yy.gcore.repository;

import com.pavlent1yy.gcore.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    Optional<PasswordResetToken> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteAllByUser_Id(Long userId);
}
