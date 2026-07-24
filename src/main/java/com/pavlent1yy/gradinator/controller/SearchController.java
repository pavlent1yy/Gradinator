package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.service.ScheduleSearchService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@AllArgsConstructor
public class SearchController {

    private final ScheduleSearchService searchService;

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String teacher,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer pair
    ) {
        if (teacher == null && group == null && room == null && subject == null && date == null && pair == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Нужен хотя бы один параметр поиска"));
        }

        LocalDate parsedDate = date != null ? LocalDate.parse(date) : null;
        List<ScheduleEntry> result = searchService.search(teacher, group, room, subject, parsedDate, pair);
        return ResponseEntity.ok(result);
    }
}
