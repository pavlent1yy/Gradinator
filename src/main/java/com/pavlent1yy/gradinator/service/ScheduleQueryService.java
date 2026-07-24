package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.dto.DayScheduleResponse;
import com.pavlent1yy.gradinator.dto.PairResponse;
import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ScheduleQueryService {

    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;

    @Transactional(readOnly = true)
    public Optional<DayScheduleResponse> getForGroup(String group, LocalDate date) {
        var snapshot = snapshotRepository.findByScheduleDate(date);
        if (snapshot.isEmpty()) return Optional.empty();

        List<ScheduleEntry> entries = entryRepository.findBySnapshot_Id(snapshot.get().getId()).stream()
                .filter(e -> e.getGroupName().equals(group))
                .sorted(Comparator.comparingInt(ScheduleEntry::getPairNumber))
                .toList();

        if (entries.isEmpty()) return Optional.empty();

        return Optional.of(toResponse(group, date, entries));
    }

    @Transactional(readOnly = true)
    public Map<String, DayScheduleResponse> getForAllGroups(LocalDate date) {
        var snapshot = snapshotRepository.findByScheduleDate(date);
        if (snapshot.isEmpty()) return Map.of();

        Map<String, List<ScheduleEntry>> byGroup = entryRepository.findBySnapshot_Id(snapshot.get().getId()).stream()
                .collect(Collectors.groupingBy(ScheduleEntry::getGroupName));

        Map<String, DayScheduleResponse> result = new HashMap<>();
        for (var e : byGroup.entrySet()) {
            List<ScheduleEntry> sorted = e.getValue().stream()
                    .sorted(Comparator.comparingInt(ScheduleEntry::getPairNumber))
                    .toList();
            result.put(e.getKey(), toResponse(e.getKey(), date, sorted));
        }
        return result;
    }

    private DayScheduleResponse toResponse(String group, LocalDate date, List<ScheduleEntry> entries) {
        List<PairResponse> pairs = entries.stream()
                .map(e -> new PairResponse(
                        e.getPairNumber(),
                        toCellData(e.getNumeratorSubjects(), e.getNumeratorTeachers(), e.getNumeratorRooms()),
                        toCellData(e.getDenominatorSubjects(), e.getDenominatorTeachers(), e.getDenominatorRooms()),
                        e.isHasChanges()
                ))
                .toList();

        return new DayScheduleResponse(group, entries.get(0).getDay(), date, pairs);
    }

    private CellData toCellData(List<String> subjects, List<String> teachers, List<String> rooms) {
        List<String> s = new ArrayList<>(subjects);
        List<String> t = new ArrayList<>(teachers);
        List<String> r = new ArrayList<>(rooms);

        boolean allEmpty = s.isEmpty() && t.isEmpty() && r.isEmpty();
        return allEmpty ? null : new CellData(s, t, r);
    }
}