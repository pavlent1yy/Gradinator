package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.entity.HeartbeatLog;

import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.service.parser.ExcelFileSyncService;
import com.pavlent1yy.gradinator.service.parser.WebParserService;
import com.pavlent1yy.gradinator.service.parser.WebParserService.ShiftChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final WebParserService parserService;
    private final ExcelFileSyncService excelFileSyncService;
    private final GroupSyncService groupSyncService;
    private final HeartbeatLogRepository heartbeatLogRepository;
    private final SnapshotBuildService snapshotBuildService;
    private final WeekService weekService;

    @Value("${api.start-with-heartbeat}")
    private boolean startWithHeartbeat;

    @EventListener(ApplicationReadyEvent.class)
    public void startup() {
        if (startWithHeartbeat) {
            log.info("🔵Первый heartbeat после запуска приложения api.start-with-heartbeat={}", startWithHeartbeat);
            run();
        }
    }

    @Scheduled(initialDelay = 15 * 60 * 1000, fixedDelay = 15 * 60 * 1000)
    public void run() {
        Instant start = Instant.now();
        log.info("🔵Heartbeat стартовал: {}", start);
        StringBuilder message = new StringBuilder();

        try {
//            excelFileSyncService.syncAll();

            var allChanges = parserService.getAllChanges();
            LocalDate today = LocalDate.now();
            List<String> groups = groupSyncService.syncAndGetGroups().stream()
                    .map(Group::getName)
                    .toList();

            Map<LocalDate, List<ShiftChanges>> changesByDate = new TreeMap<>();
            changesByDate.put(today, new ArrayList<>());
            for (ShiftChanges changes : allChanges) {
                if (!changes.date().isBefore(today)) {
                    changesByDate.computeIfAbsent(changes.date(), date -> new ArrayList<>()).add(changes);
                }
            }

            SnapshotBuiltStatus todayStatus = buildSnapshot(today, groups, changesByDate.remove(today));
            message.append("""
                Status: %s
                Groups: %d
                Snapshot: %s
                """.formatted(todayStatus, groups.size(), today));

            for (var entry : changesByDate.entrySet()) {
                LocalDate date = entry.getKey();
                if (weekService.isDayOff(date)) continue;
                var aheadStatus = buildSnapshot(date, groups, entry.getValue());
                message.append("\nAhead: ").append(date).append(" ").append(aheadStatus);
            }

            boolean complete = allChanges.size() == WebParserService.Shift.values().length;
            if (!complete) message.append("\nНе удалось загрузить замены для всех смен");
            saveLog(start, complete ? HeartbeatLog.Status.SUCCESS : HeartbeatLog.Status.ERROR, message.toString());

        } catch (Exception e) {
            log.error("⭕Heartbeat упал", e);
            saveLog(start, HeartbeatLog.Status.ERROR, e.getMessage());
        }
    }

    private SnapshotBuiltStatus buildSnapshot(LocalDate date, List<String> groups, List<ShiftChanges> published) {
        if (weekService.isDayOff(date) || (published.isEmpty() && snapshotBuildService.existsForDate(date))) {
            return SnapshotBuiltStatus.NO_CHANGES;
        }

        Map<String, List<PairSlot>> changesByGroup = new HashMap<>();
        for (ShiftChanges changes : published) {
            changes.byGroup().forEach((group, pairs) ->
                    changesByGroup.computeIfAbsent(group, key -> new ArrayList<>()).addAll(pairs));
        }

        Set<String> preserveChangesFor = new HashSet<>();
        if (published.size() < WebParserService.Shift.values().length) {
            for (String group : groups) {
                if (!changesByGroup.containsKey(group) &&
                        published.stream().noneMatch(changes -> changes.shift().includes(group))) {
                    preserveChangesFor.add(group);
                }
            }
        }
        return snapshotBuildService.buildAndSave(date, groups, changesByGroup, preserveChangesFor);
    }

    private void saveLog(Instant start, HeartbeatLog.Status status, String message) {
        HeartbeatLog heartbeatLog = HeartbeatLog.builder()
                .startedAt(LocalDateTime.now().minusNanos(Duration.between(start, Instant.now()).toNanos()))
                .finishedAt(LocalDateTime.now())
                .status(status)
                .message(message)
                .build();
        heartbeatLog = heartbeatLogRepository.save(heartbeatLog);
        log.info("\n\n❤ Heartbeat #{}\n{}", heartbeatLog.getId(), heartbeatLog.getMessage());
    }



}
