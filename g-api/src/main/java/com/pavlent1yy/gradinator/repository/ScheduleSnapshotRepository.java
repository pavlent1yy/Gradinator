package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ScheduleSnapshotRepository extends JpaRepository<ScheduleSnapshot, Long> {
    Optional<ScheduleSnapshot> findByScheduleDate(LocalDate scheduleDate);
}