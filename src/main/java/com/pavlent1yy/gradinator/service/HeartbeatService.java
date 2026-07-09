package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.GroupEntityRepository;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class HeartbeatService {

    private final ExcelFileSyncService excelFileSyncService;
    private final ScheduleWebParserService parserService;
    private final ScheduleService scheduleService;
    private final SnapshotMapper snapshotMapper;
    private final ScheduleSnapshotRepository snapshotRepository;
    private final ScheduleEntryRepository entryRepository;
    private final HeartbeatLogRepository heartbeatLogRepository;
    private final SnapshotBuildService snapshotBuildService;

    private enum Status { NO_CHANGES, CREATED, UPDATED }

    @PostConstruct
    public void init() {
        run();
    }

    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void run() {
        Instant start = Instant.now();
        StringBuilder message = new StringBuilder();

        try {
            excelFileSyncService.syncAll();

            var allChanges = parserService.getAllChanges();
            LocalDate today = LocalDate.now();
            LocalDate changesDate = allChanges.date();

            List<String> groups = scheduleService.getAllGroups();

            Map<String, List<PairSlot>> todaysChanges = changesDate != null && changesDate.equals(today)
                    ? allChanges.byGroup()
                    : Map.of();

            var todayStatus = snapshotBuildService.buildAndSave(today, groups, todaysChanges);

            message.append("Status: ").append(todayStatus)
                    .append("\nDuration: ").append(formatDuration(start))
                    .append("\nGroups: ").append(groups.size())
                    .append("\nSnapshot: ").append(today);

            if (changesDate != null && changesDate.isAfter(today)) {
                var aheadStatus = snapshotBuildService.buildAndSave(changesDate, groups, allChanges.byGroup());
                message.append("\nAhead: ").append(changesDate).append(" ").append(aheadStatus);
            }

            saveLog(start, HeartbeatLog.Status.SUCCESS, message.toString());

        } catch (Exception e) {
            log.error("Heartbeat упал", e);
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

        log.info("Heartbeat #{}\n{}", heartbeatLog.getId(), heartbeatLog.getMessage());
    }

    private String formatDuration(Instant start) {
        return String.format("%.1f sec", Duration.between(start, Instant.now()).toMillis() / 1000.0);
    }


}