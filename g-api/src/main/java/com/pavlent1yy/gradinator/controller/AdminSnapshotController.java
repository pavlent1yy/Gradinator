package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/snapshots")
@AllArgsConstructor
public class AdminSnapshotController {

    private final ScheduleSnapshotRepository snapshotRepository;

    @GetMapping
    public List<ScheduleSnapshot> getAll() {
        return snapshotRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleSnapshot> getById(@PathVariable Long id) {
        return snapshotRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}