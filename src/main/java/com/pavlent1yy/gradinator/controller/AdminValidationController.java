package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.dto.SnapshotValidationResult;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import com.pavlent1yy.gradinator.service.SnapshotValidationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/validation")
@AllArgsConstructor
public class AdminValidationController {

    private final ScheduleSnapshotRepository snapshotRepository;
    private final SnapshotValidationService validationService;

    @GetMapping
    public List<SnapshotValidationResult> validateAll() {
        return snapshotRepository.findAll().stream()
                .map(validationService::validate)
                .toList();
    }

    @GetMapping("/{snapshotId}")
    public ResponseEntity<SnapshotValidationResult> validateOne(@PathVariable Long snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .map(validationService::validate)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}