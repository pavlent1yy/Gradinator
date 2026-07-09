package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class SnapshotBuildService {

    private final ScheduleService scheduleService;
    private final SnapshotMapper snapshotMapper;
    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;

    public enum Status { NO_CHANGES, CREATED, UPDATED }

    @Transactional
    public Status buildAndSave(LocalDate date, List<String> groups, Map<String, List<PairSlot>> changesByGroup) {
        Map<String, DaySchedule> byGroup = new HashMap<>();
        for (String g : groups) {
            try {
                byGroup.put(g, scheduleService.getScheduleForDate(g, date, changesByGroup.getOrDefault(g, List.of())));
            } catch (Exception e) {
                log.error("Не удалось построить расписание для группы {}, пропускаю", g, e);
            }
        }

        String newHash = snapshotMapper.computeHash(byGroup);
        var existing = snapshotRepository.findByScheduleDate(date);

        if (existing.isPresent() && existing.get().getHash().equals(newHash)) {
            return Status.NO_CHANGES;
        }

        Status status = existing.isPresent() ? Status.UPDATED : Status.CREATED;

        ScheduleSnapshot snapshot = existing.orElseGet(() -> ScheduleSnapshot.builder().scheduleDate(date).build());
        snapshot.setHash(newHash);
        snapshot = snapshotRepository.save(snapshot);

        if (existing.isPresent()) {
            entryRepository.deleteBySnapshot_Id(snapshot.getId());
        }
        entryRepository.saveAll(snapshotMapper.toEntries(snapshot, byGroup));

        return status;
    }
}