package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/heartbeat")
@AllArgsConstructor
public class HeartbeatController {

    private final HeartbeatLogRepository heartbeatLogRepository;
    private final ScheduleSnapshotRepository snapshotRepository;

    @GetMapping("/latest")
    public ResponseEntity<HeartbeatLog> getLatest() {
        return heartbeatLogRepository
                .findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/logs")
    public List<HeartbeatLog> getLogs(@RequestParam(defaultValue = "20") int limit) {
        return heartbeatLogRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id")))
                .getContent();
    }

    @GetMapping("/dates")
    public List<String> getAvailableDates() {
        return snapshotRepository.findAll().stream()
                .map(ScheduleSnapshot::getScheduleDate)
                .sorted()
                .map(Object::toString)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeartbeatLog> getById(@PathVariable Long id) {
        return heartbeatLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}