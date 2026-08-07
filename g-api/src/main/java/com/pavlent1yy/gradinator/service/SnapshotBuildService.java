package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class SnapshotBuildService {

    private final ScheduleService scheduleService;
    private final SnapshotMapper snapshotMapper;
    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;

    @Transactional
    public SnapshotBuiltStatus buildAndSave(LocalDate date, List<String> groups, Map<String, List<PairSlot>> changesByGroup) {
        Map<String, DaySchedule> byGroup = new HashMap<>();
        Map<String, Set<Integer>> changedPairsByGroup = new HashMap<>();

        for (String g : groups) {
            try {
                List<PairSlot> changes = changesByGroup.getOrDefault(g, List.of());
                changedPairsByGroup.put(g, changes.stream().map(PairSlot::getPairNumber).collect(Collectors.toSet()));
                byGroup.put(g, scheduleService.getScheduleForDate(g, date, changes));
            } catch (Exception e) {
                log.error("⭕Не удалось построить расписание для группы {}, пропускаю", g, e);
            }
        }

        String newHash = snapshotMapper.computeHash(byGroup);
        Optional<ScheduleSnapshot> existing = snapshotRepository.findByScheduleDate(date);

        if (existing.isPresent() && existing.get().getHash().equals(newHash)) {
            return SnapshotBuiltStatus.NO_CHANGES;
        }

        SnapshotBuiltStatus status = existing.isPresent() ? SnapshotBuiltStatus.UPDATED : SnapshotBuiltStatus.CREATED;

        ScheduleSnapshot snapshot = existing.orElseGet(() -> ScheduleSnapshot.builder().scheduleDate(date).build());
        snapshot.setHash(newHash);
        snapshot = snapshotRepository.save(snapshot);

        if (existing.isPresent()) {
            entryRepository.deleteBySnapshot_Id(snapshot.getId());
        }
        entryRepository.saveAll(snapshotMapper.toEntries(snapshot, byGroup, changedPairsByGroup));

        return status;
    }
}