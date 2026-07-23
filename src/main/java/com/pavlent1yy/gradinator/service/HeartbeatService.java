package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;

import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.GroupEntityRepository;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final ExcelFileSyncService excelFileSyncService;
    private final ScheduleWebParserService parserService;
    private final ScheduleService scheduleService;
    private final GroupEntityRepository groupRepository;
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

    @Scheduled(
            initialDelay = 15 * 60 * 1000,
            fixedDelay = 15 * 60 * 1000
    )
    public void run() {
        Instant start = Instant.now();
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log.info("🔵Heartbeat. Время: {} | следующий в {}", dtFormatter.format(LocalTime.now()),
                dtFormatter.format(LocalTime.now().plusMinutes(15)));
        StringBuilder message = new StringBuilder();

        try {
            excelFileSyncService.syncAll();

            var allChanges = parserService.getAllChanges();
            LocalDate today = LocalDate.now();
            LocalDate changesDate = allChanges.date();

            List<String> groups = groupRepository.findAll().stream()
                    .map(com.pavlent1yy.gradinator.entity.GroupEntity::getName)
                    .toList();

            scheduleService.checkGroupSync(new HashSet<>(scheduleService.getAllGroupsFromFiles()), new HashSet<>(groups));

            Map<String, List<PairSlot>> todaysChanges = changesDate != null && changesDate.equals(today)
                    ? allChanges.byGroup()
                    : Map.of();

            var todayStatus = snapshotBuildService.buildAndSave(today, groups, todaysChanges);

            message.append( """
            Status   : %s
            Duration : %s
            Groups   : %d
            Snapshot : %s
            """.formatted(
                    todayStatus,
                    formatDuration(start),
                    groups.size(),
                    today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));

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

        log.info("\n\n❤ Heartbeat #{}\n{}\n", heartbeatLog.getId(), heartbeatLog.getMessage());
    }

    private String formatDuration(Instant start) {
        return String.format("%.1f sec", Duration.between(start, Instant.now()).toMillis() / 1000.0);
    }


}