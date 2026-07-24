package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/heartbeats")
@AllArgsConstructor
public class AdminHeartbeatController {

    private final HeartbeatLogRepository heartbeatLogRepository;

    @GetMapping
    public List<HeartbeatLog> getAll(@RequestParam(defaultValue = "50") int limit) {
        return heartbeatLogRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    }

    @GetMapping("/latest")
    public ResponseEntity<HeartbeatLog> getLatest() {
        return heartbeatLogRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .stream().findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeartbeatLog> getById(@PathVariable Long id) {
        return heartbeatLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}