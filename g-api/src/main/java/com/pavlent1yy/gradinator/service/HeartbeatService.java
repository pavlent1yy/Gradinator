package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.entity.HeartbeatLog;

import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.service.parser.ExcelFileSyncService;
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
            excelFileSyncService.syncAll();

            var allChanges = parserService.getAllChanges();
            LocalDate today = LocalDate.now();
            LocalDate changesDate = allChanges.date();

            List<String> groups = groupSyncService.syncAndGetGroups().stream()
                    .map(Group::getName)
                    .toList();

            boolean changesCoverToday = changesDate != null && changesDate.equals(today);
            Map<String, List<PairSlot>> todaysChanges = changesCoverToday
                    ? allChanges.byGroup()
                    : Map.of();

            SnapshotBuiltStatus todayStatus;
            if (!changesCoverToday && snapshotBuildService.existsForDate(today)) {
                todayStatus = SnapshotBuiltStatus.NO_CHANGES;
                log.debug("🐜 Снапшот на {} уже есть, замены. Пропускаю пересчёт", today);
            } else {
                todayStatus = snapshotBuildService.buildAndSave(today, groups, todaysChanges);
            }

            message.append("""
                Status: %s
                Groups: %d
                Snapshot: %s
                """.formatted(todayStatus, groups.size(), today));

            if (changesDate != null && changesDate.isAfter(today)) {
                var aheadStatus = snapshotBuildService.buildAndSave(changesDate, groups, allChanges.byGroup());
                message.append("\nAhead: ").append(changesDate).append(" ").append(aheadStatus);
            }

            saveLog(start, HeartbeatLog.Status.SUCCESS, message.toString());

        } catch (Exception e) {
            log.error("⭕Heartbeat упал", e);
            saveLog(start, HeartbeatLog.Status.ERROR, e.getMessage());
        }
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