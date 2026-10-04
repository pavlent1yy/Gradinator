package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.entity.HeartbeatLog;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.repository.HeartbeatLogRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import com.pavlent1yy.gradinator.service.parser.WebParserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminControllersTest {

    private final HeartbeatLogRepository heartbeatLogRepository = mock(HeartbeatLogRepository.class);
    private final ScheduleSnapshotRepository snapshotRepository = mock(ScheduleSnapshotRepository.class);

    private final HeartbeatController heartbeatController =
            new HeartbeatController(heartbeatLogRepository, snapshotRepository);
    private final AdminSnapshotController snapshotController = new AdminSnapshotController(snapshotRepository);

    private static HeartbeatLog log(long id) {
        return HeartbeatLog.builder().id(id).status(HeartbeatLog.Status.SUCCESS).build();
    }

    private Pageable requestedPage() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(heartbeatLogRepository).findAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void latestHeartbeatIsNewestById() {
        when(heartbeatLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log(9))));

        var response = heartbeatController.getLatest();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getId()).isEqualTo(9L);
        assertThat(requestedPage()).isEqualTo(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Test
    void latestHeartbeatIs404WhenNoLogs() {
        when(heartbeatLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(heartbeatController.getLatest().getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void logsLimitIsClampedTo1_100() {
        when(heartbeatLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(log(1))));

        heartbeatController.getLogs(500);
        heartbeatController.getLogs(0);
        heartbeatController.getLogs(20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(heartbeatLogRepository, times(3)).findAll(captor.capture());
        assertThat(captor.getAllValues()).extracting(Pageable::getPageSize).containsExactly(100, 1, 20);
    }

    @Test
    void availableDatesAreSortedStrings() {
        when(snapshotRepository.findAll()).thenReturn(List.of(
                ScheduleSnapshot.builder().scheduleDate(LocalDate.of(2026, 10, 6)).build(),
                ScheduleSnapshot.builder().scheduleDate(LocalDate.of(2026, 10, 5)).build()));

        assertThat(heartbeatController.getAvailableDates()).containsExactly("2026-10-05", "2026-10-06");
    }

    @Test
    void heartbeatById() {
        when(heartbeatLogRepository.findById(1L)).thenReturn(Optional.of(log(1)));
        when(heartbeatLogRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(heartbeatController.getById(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(heartbeatController.getById(2L).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void snapshotsListAndById() {
        ScheduleSnapshot snapshot = ScheduleSnapshot.builder().id(1L).build();
        when(snapshotRepository.findAll()).thenReturn(List.of(snapshot));
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(snapshotController.getAll()).containsExactly(snapshot);
        assertThat(snapshotController.getById(1L).getBody()).isSameAs(snapshot);
        assertThat(snapshotController.getById(2L).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void parserControllerReturnsChangeDate() {
        WebParserService parser = mock(WebParserService.class);
        when(parser.getDateFromChangesURL()).thenReturn(LocalDate.of(2026, 10, 5));

        assertThat(new WebParserController(parser).getActualChangeDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    }
}
