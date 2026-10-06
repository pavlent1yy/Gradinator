package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.records.AbsenceDayRequest;
import com.pavlent1yy.gcore.dto.records.AbsenceRequest;
import com.pavlent1yy.gcore.dto.records.AbsenceResponse;
import com.pavlent1yy.gcore.dto.records.AbsenceStatsResponse;
import com.pavlent1yy.gcore.service.AbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core/absences")
@PreAuthorize("isAuthenticated()")
public class AbsenceController {

    private final AbsenceService absenceService;

    @GetMapping
    public List<AbsenceResponse> list(Authentication authentication,
                                      @RequestParam LocalDate from,
                                      @RequestParam LocalDate to) {
        return absenceService.list(authentication.getName(), from, to);
    }

    @PutMapping
    public AbsenceResponse mark(Authentication authentication, @RequestBody AbsenceRequest request) {
        return absenceService.mark(authentication.getName(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> unmark(Authentication authentication,
                                       @RequestParam LocalDate date,
                                       @RequestParam Integer pairNumber) {
        absenceService.unmark(authentication.getName(), date, pairNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/day")
    public List<AbsenceResponse> markDay(Authentication authentication, @RequestBody AbsenceDayRequest request) {
        return absenceService.markDay(authentication.getName(), request.date());
    }

    @DeleteMapping("/day")
    public ResponseEntity<Void> clearDay(Authentication authentication, @RequestParam LocalDate date) {
        absenceService.clearDay(authentication.getName(), date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public AbsenceStatsResponse stats(Authentication authentication) {
        return absenceService.stats(authentication.getName());
    }
}
