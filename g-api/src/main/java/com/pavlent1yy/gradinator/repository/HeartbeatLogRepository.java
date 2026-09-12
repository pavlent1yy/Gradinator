package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeartbeatLogRepository extends JpaRepository<HeartbeatLog, Long> {
}