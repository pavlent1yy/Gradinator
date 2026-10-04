package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.service.parser.ExcelFileSyncService;
import com.pavlent1yy.gradinator.service.parser.WebParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HeartbeatServiceTest {

    @Mock
    private WebParserService parserService;

    @Mock
    private ExcelFileSyncService excelFileSyncService;

    @Mock
    private GroupSyncService groupSyncService;

    @Mock
    private HeartbeatLogRepository heartbeatLogRepository;

    @Mock
    private SnapshotBuildService snapshotBuildService;

    @Mock
    private WeekService weekService;

    @InjectMocks
    private HeartbeatService service;

    private final LocalDate today = LocalDate.now();
    private final List<String> groups = List.of("ИС1-33", "СА1-21");
    private final Map<String, List<PairSlot>> changes = Map.of("ИС1-33", List.of(new PairSlot()));

    @BeforeEach
    void setUp() {
        when(groupSyncService.syncAndGetGroups()).thenReturn(List.of(
                Group.builder().name("ИС1-33").build(),
                Group.builder().name("СА1-21").build()));
        when(heartbeatLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(weekService.isDayOff(any())).thenReturn(false);
        when(snapshotBuildService.buildAndSave(any(), anyList(), anyMap())).thenReturn(SnapshotBuiltStatus.CREATED);
    }

    private void changesFor(LocalDate date) {
        when(parserService.getAllChanges()).thenReturn(new WebParserService.AllChanges(date, changes));
    }

    private HeartbeatLog savedLog() {
        ArgumentCaptor<HeartbeatLog> captor = ArgumentCaptor.forClass(HeartbeatLog.class);
        verify(heartbeatLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void buildsTodayWithChangesWhenTheyAreForToday() {
        changesFor(today);

        service.run();

        verify(snapshotBuildService).buildAndSave(today, groups, changes);
        HeartbeatLog log = savedLog();
        assertThat(log.getStatus()).isEqualTo(HeartbeatLog.Status.SUCCESS);
        assertThat(log.getMessage()).contains("Status: CREATED").contains("Groups: 2");
        assertThat(log.getStartedAt()).isBeforeOrEqualTo(log.getFinishedAt());
    }

    @Test
    void skipsRebuildWhenChangesAreOldAndSnapshotExists() {
        changesFor(today.minusDays(1));
        when(snapshotBuildService.existsForDate(today)).thenReturn(true);

        service.run();

        verify(snapshotBuildService, never()).buildAndSave(any(), anyList(), anyMap());
        assertThat(savedLog().getMessage()).contains("Status: NO_CHANGES");
    }

    @Test
    void buildsTodayWithoutChangesWhenSnapshotMissing() {
        changesFor(today.minusDays(1));
        when(snapshotBuildService.existsForDate(today)).thenReturn(false);

        service.run();

        verify(snapshotBuildService).buildAndSave(today, groups, Map.of());
    }

    @Test
    void buildsTodayWithoutChangesWhenChangesDateUnknown() {
        changesFor(null);

        service.run();

        verify(snapshotBuildService).buildAndSave(today, groups, Map.of());
        assertThat(savedLog().getMessage()).doesNotContain("Ahead");
    }

    @Test
    void doesNotBuildOnDayOff() {
        changesFor(today);
        when(weekService.isDayOff(today)).thenReturn(true);

        service.run();

        verify(snapshotBuildService, never()).buildAndSave(eq(today), anyList(), anyMap());
        assertThat(savedLog().getMessage()).contains("Status: NO_CHANGES");
    }

    @Test
    void buildsAheadSnapshotForFutureChanges() {
        LocalDate tomorrow = today.plusDays(1);
        changesFor(tomorrow);
        when(snapshotBuildService.existsForDate(today)).thenReturn(true);
        when(snapshotBuildService.buildAndSave(eq(tomorrow), anyList(), anyMap())).thenReturn(SnapshotBuiltStatus.UPDATED);

        service.run();

        verify(snapshotBuildService).buildAndSave(tomorrow, groups, changes);
        assertThat(savedLog().getMessage()).contains("Ahead: " + tomorrow + " UPDATED");
    }

    @Test
    void doesNotBuildAheadForFutureDayOff() {
        LocalDate tomorrow = today.plusDays(1);
        changesFor(tomorrow);
        when(snapshotBuildService.existsForDate(today)).thenReturn(true);
        when(weekService.isDayOff(tomorrow)).thenReturn(true);

        service.run();

        verify(snapshotBuildService, never()).buildAndSave(eq(tomorrow), anyList(), anyMap());
    }

    @Test
    void logsErrorWhenSomethingFails() {
        when(parserService.getAllChanges()).thenThrow(new IllegalStateException("сайт лежит"));

        service.run();

        HeartbeatLog log = savedLog();
        assertThat(log.getStatus()).isEqualTo(HeartbeatLog.Status.ERROR);
        assertThat(log.getMessage()).isEqualTo("сайт лежит");
    }

    @Test
    void startupRunsOnlyWhenEnabled() {
        changesFor(today);

        ReflectionTestUtils.setField(service, "startWithHeartbeat", false);
        service.startup();
        verifyNoInteractions(parserService);

        ReflectionTestUtils.setField(service, "startWithHeartbeat", true);
        service.startup();
        verify(parserService).getAllChanges();
    }
}
