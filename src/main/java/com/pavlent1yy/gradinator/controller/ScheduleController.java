package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
@RestController
@AllArgsConstructor
public class ScheduleController {

    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;

    @GetMapping("/schedule/{group}")
    public ResponseEntity<DaySchedule> getSchedule(
            @PathVariable String group,
            @RequestParam(defaultValue = "today") String date
    ) {
        LocalDate target = switch (date) {
            case "today" -> LocalDate.now();
            case "tomorrow" -> LocalDate.now().plusDays(1);
            default -> LocalDate.parse(date);
        };

        var snapshot = snapshotRepository.findByScheduleDate(target);
        if (snapshot.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<ScheduleEntry> entries = entryRepository.findBySnapshot_Id(snapshot.get().getId()).stream()
                .filter(e -> e.getGroupName().equals(group))
                .sorted(Comparator.comparingInt(ScheduleEntry::getPairNumber))
                .toList();

        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DaySchedule result = new DaySchedule(entries.get(0).getDay());
        for (ScheduleEntry e : entries) {
            CellData numerator = toCellData(e.getNumeratorSubjects(), e.getNumeratorTeachers(), e.getNumeratorRooms());
            CellData denominator = toCellData(e.getDenominatorSubjects(), e.getDenominatorTeachers(), e.getDenominatorRooms());
            result.getPairs().add(new PairSlot(e.getPairNumber(), numerator, denominator));
        }

        return ResponseEntity.ok(result);
    }

    private CellData toCellData(List<String> subjects, List<String> teachers, List<String> rooms) {
        boolean allEmpty = subjects.isEmpty() && teachers.isEmpty() && rooms.isEmpty();
        return allEmpty ? null : new CellData(subjects, teachers, rooms);
    }
}