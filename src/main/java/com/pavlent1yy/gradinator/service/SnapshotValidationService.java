package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.dto.SnapshotValidationResult;
import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SnapshotValidationService {

    private final ScheduleEntryRepository entryRepository;
    private final ScheduleService scheduleService;
    private final SnapshotMapper snapshotMapper;

    public SnapshotValidationResult validate(ScheduleSnapshot snapshot) {
        List<ScheduleEntry> entries = entryRepository.findBySnapshot_Id(snapshot.getId());

        Set<String> expectedGroups = new HashSet<>(scheduleService.getAllGroups());
        Set<String> actualGroups = entries.stream().map(ScheduleEntry::getGroupName).collect(Collectors.toSet());

        List<String> missingGroups = expectedGroups.stream()
                .filter(g -> !actualGroups.contains(g))
                .sorted()
                .toList();

        Map<String, List<ScheduleEntry>> byGroup = entries.stream()
                .collect(Collectors.groupingBy(ScheduleEntry::getGroupName));

        List<String> duplicatePairGroups = byGroup.entrySet().stream()
                .filter(e -> {
                    long distinctPairs = e.getValue().stream().map(ScheduleEntry::getPairNumber).distinct().count();
                    return distinctPairs != e.getValue().size();
                })
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        Map<String, DaySchedule> reconstructed = new HashMap<>();
        for (var e : byGroup.entrySet()) {
            DaySchedule day = new DaySchedule(e.getValue().isEmpty() ? "" : e.getValue().get(0).getDay());
            for (ScheduleEntry entry : e.getValue()) {
                CellData num = toCellData(entry.getNumeratorSubjects(), entry.getNumeratorTeachers(), entry.getNumeratorRooms());
                CellData den = toCellData(entry.getDenominatorSubjects(), entry.getDenominatorTeachers(), entry.getDenominatorRooms());
                day.getPairs().add(new PairSlot(entry.getPairNumber(), num, den));
            }
            reconstructed.put(e.getKey(), day);
        }

        String recomputedHash = snapshotMapper.computeHash(reconstructed);
        boolean hashMatches = recomputedHash.equals(snapshot.getHash());

        return new SnapshotValidationResult(
                snapshot.getId(),
                snapshot.getScheduleDate().toString(),
                hashMatches,
                missingGroups,
                duplicatePairGroups,
                entries.size()
        );
    }

    private CellData toCellData(List<String> subjects, List<String> teachers, List<String> rooms) {
        boolean allEmpty = subjects.isEmpty() && teachers.isEmpty() && rooms.isEmpty();
        return allEmpty ? null : new CellData(subjects, teachers, rooms);
    }
}