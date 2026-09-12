package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {
    List<ScheduleEntry> findBySnapshot_Id(Long snapshotId);
    void deleteBySnapshot_Id(Long snapshotId);
}