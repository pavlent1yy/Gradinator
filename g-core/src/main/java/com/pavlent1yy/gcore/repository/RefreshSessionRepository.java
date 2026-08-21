package com.pavlent1yy.gcore.repository;

import com.pavlent1yy.gcore.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshSessionRepository
        extends JpaRepository<RefreshSession, Long> {

    @Query("""
        SELECT rs
        FROM RefreshSession rs
        JOIN FETCH rs.user
        WHERE rs.refreshTokenHash = :hash
    """)
    Optional<RefreshSession> findByRefreshTokenHash(@Param("hash") String hash);
}
