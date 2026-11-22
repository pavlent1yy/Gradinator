package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SnapshotBuildService {

    private final ScheduleService scheduleService;
    private final SnapshotMapper snapshotMapper;
    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;
    private final CacheManager cacheManager;

    @Transactional
    public SnapshotBuiltStatus buildAndSave(
            LocalDate date,
            List<String> groups,
            Map<String, List<PairSlot>> changesByGroup,
            Set<String> preserveChangesFor
    ) {
        if (groups.isEmpty()) throw new IllegalStateException("Нет групп для построения расписания на " + date);

        Optional<ScheduleSnapshot> existing = snapshotRepository.findByScheduleDate(date);
        Map<String, List<PairSlot>> effectiveChanges = new HashMap<>(changesByGroup);
        if (existing.isPresent() && !preserveChangesFor.isEmpty()) {
            for (var entry : entryRepository.findBySnapshot_Id(existing.get().getId())) {
                if (entry.isHasChanges() && preserveChangesFor.contains(entry.getGroupName())) {
                    effectiveChanges.computeIfAbsent(entry.getGroupName(), group -> new ArrayList<>())
                            .add(snapshotMapper.toPairSlot(entry));
                }
            }
        }

        Map<String, DaySchedule> byGroup = new HashMap<>();
        Map<String, Set<Integer>> changedPairsByGroup = new HashMap<>();

        for (String g : groups) {
            try {
                List<PairSlot> changes = effectiveChanges.getOrDefault(g, List.of());

                changedPairsByGroup.put(
                        g,
                        changes.stream()
                                .map(PairSlot::getPairNumber)
                                .collect(Collectors.toSet())
                );

                byGroup.put(
                        g,
                        scheduleService.getScheduleForDate(g, date, changes)
                );

            } catch (RuntimeException e) {
                throw new IllegalStateException("Не удалось построить расписание для группы " + g + " на " + date, e);
            }
        }

        String newHash = snapshotMapper.computeHash(byGroup, changedPairsByGroup);

        if (existing.isPresent() && existing.get().getHash().equals(newHash)) {
            return SnapshotBuiltStatus.NO_CHANGES;
        }

        SnapshotBuiltStatus status = existing.isPresent()
                ? SnapshotBuiltStatus.UPDATED
                : SnapshotBuiltStatus.CREATED;

        ScheduleSnapshot snapshot = existing
                .orElseGet(() -> ScheduleSnapshot.builder()
                        .scheduleDate(date)
                        .build());

        snapshot.setHash(newHash);
        snapshot = snapshotRepository.save(snapshot);

        if (existing.isPresent()) {
            entryRepository.deleteBySnapshot_Id(snapshot.getId());
        }

        entryRepository.saveAll(
                snapshotMapper.toEntries(
                        snapshot,
                        byGroup,
                        changedPairsByGroup
                )
        );

        evictScheduleCache();

        return status;
    }

    private void evictScheduleCache() {
        cacheManager.getCache("schedule").clear();
        cacheManager.getCache("scheduleAll").clear();
    }

    public boolean existsForDate(LocalDate date) {
        return snapshotRepository.findByScheduleDate(date).isPresent();
    }
}
