package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.dto.DayScheduleResponse;
import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.service.QueryService;
import com.pavlent1yy.gradinator.service.WeekService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@AllArgsConstructor
public class ScheduleController {

    private final QueryService queryService;
    private final WeekService weekService;

    @GetMapping
    public ResponseEntity<?> getSchedule(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String date
    ) {
        LocalDate target;
        try {
            target = date == null ? LocalDate.now() : resolveDate(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Некорректная дата, ожидается yyyy-MM-dd"));
        }
        return respondForDate(group, target);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getToday(@RequestParam(required = false) String group) {
        return respondForDate(group, LocalDate.now());
    }

    @GetMapping("/tomorrow")
    public ResponseEntity<?> getTomorrow(@RequestParam(required = false) String group) {
        return respondForDate(group, LocalDate.now().plusDays(1));
    }

    @GetMapping("/yesterday")
    public ResponseEntity<?> getYesterday(@RequestParam(required = false) String group) {
        return respondForDate(group, LocalDate.now().minusDays(1));
    }


    @GetMapping("/current-weektype")
    public ResponseEntity<?> getCurrentWeekType() {
        WeekType type = weekService.getCurrentWeekType();
        String label = type == WeekType.NUMERATOR ? "Числитель" : "Знаменатель";
        return ResponseEntity.ok(Map.of("weekType", type, "label", label));
    }

    private ResponseEntity<?> respondForDate(String group, LocalDate date) {
        if (group == null || group.isBlank()) {
            Map<String, DayScheduleResponse> all = queryService.getScheduleForAllGroups(date);
            if (all.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Снапшот на дату " + date + " ещё не посчитан"));
            }
            return ResponseEntity.ok(all);
        }

        return queryService.getScheduleForGroup(group, date)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Нет актуальных данных для группы '" + group + "' на " + date)));
    }

    private LocalDate resolveDate(String date) {
        return switch (date) {
            case "today" -> LocalDate.now();
            case "tomorrow" -> LocalDate.now().plusDays(1);
            case "yesterday" -> LocalDate.now().minusDays(1);
            default -> LocalDate.parse(date);
        };
    }
}