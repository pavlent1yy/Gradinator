package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@AllArgsConstructor
public class ScheduleController {

    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;

    @GetMapping("/{group}")
    public ResponseEntity<?> getSchedule(
            @PathVariable String group,
            @RequestParam(defaultValue = "today") String date
    ) {
        LocalDate target;
        try {
            target = resolveDate(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Некорректный формат даты, ожидается yyyy-MM-dd, today или tomorrow"));
        }

        var snapshot = snapshotRepository.findByScheduleDate(target);
        if (snapshot.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Снапшот на дату " + target + " ещё не посчитан"));
        }

        List<ScheduleEntry> entries = entryRepository.findBySnapshot_Id(snapshot.get().getId()).stream()
                .filter(e -> e.getGroupName().equals(group))
                .sorted(Comparator.comparingInt(ScheduleEntry::getPairNumber))
                .toList();

        if (entries.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Группа '" + group + "' не найдена в снапшоте на " + target));
        }

        DaySchedule result = new DaySchedule(entries.get(0).getDay());
        for (ScheduleEntry e : entries) {
            CellData numerator = toCellData(e.getNumeratorSubjects(), e.getNumeratorTeachers(), e.getNumeratorRooms());
            CellData denominator = toCellData(e.getDenominatorSubjects(), e.getDenominatorTeachers(), e.getDenominatorRooms());
            result.getPairs().add(new PairSlot(e.getPairNumber(), numerator, denominator));
        }

        return ResponseEntity.ok(result);
    }

    private LocalDate resolveDate(String date) {
        return switch (date) {
            case "today" -> LocalDate.now();
            case "tomorrow" -> LocalDate.now().plusDays(1);
            default -> LocalDate.parse(date);
        };
    }

    private CellData toCellData(List<String> subjects, List<String> teachers, List<String> rooms) {
        boolean allEmpty = subjects.isEmpty() && teachers.isEmpty() && rooms.isEmpty();
        return allEmpty ? null : new CellData(subjects, teachers, rooms);
    }
}