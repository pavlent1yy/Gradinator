package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.ScheduleFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleFileRepository extends JpaRepository<ScheduleFile, Long> {
    Optional<ScheduleFile> findByFilename(String filename);
}