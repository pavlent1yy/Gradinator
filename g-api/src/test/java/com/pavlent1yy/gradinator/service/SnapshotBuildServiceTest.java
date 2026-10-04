package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.enums.SnapshotBuiltStatus;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnapshotBuildServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 5);

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private ScheduleSnapshotRepository snapshotRepository;

    @Mock
    private ScheduleEntryRepository entryRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache scheduleCache;

    @Mock
    private Cache scheduleAllCache;

    private SnapshotBuildService service;

    @BeforeEach
    void setUp() {
        service = new SnapshotBuildService(scheduleService, new SnapshotMapper(),
                snapshotRepository, entryRepository, cacheManager);

        when(cacheManager.getCache("schedule")).thenReturn(scheduleCache);
        when(cacheManager.getCache("scheduleAll")).thenReturn(scheduleAllCache);
        when(snapshotRepository.save(any())).thenAnswer(inv -> {
            ScheduleSnapshot s = inv.getArgument(0);
            if (s.getId() == null) s.setId(42L);
            return s;
        });
    }

    private static DaySchedule day(String subject) {
        DaySchedule day = new DaySchedule("Понедельник");
        day.addPair(new PairSlot(1, new CellData(subject, "101", "Иванов"), null));
        return day;
    }

    private String hashOf(Map<String, DaySchedule> byGroup) {
        return new SnapshotMapper().computeHash(byGroup);
    }

    @SuppressWarnings("unchecked")
    private List<ScheduleEntry> savedEntries() {
        ArgumentCaptor<List<ScheduleEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(entryRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void createsSnapshotWhenNoneExists() {
        when(scheduleService.getScheduleForDate(eq("ИС1-33"), eq(DATE), anyList())).thenReturn(day("Математика"));
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.empty());

        SnapshotBuiltStatus status = service.buildAndSave(DATE, List.of("ИС1-33"), Map.of());

        assertThat(status).isEqualTo(SnapshotBuiltStatus.CREATED);
        verify(entryRepository, never()).deleteBySnapshot_Id(any());
        assertThat(savedEntries()).singleElement().satisfies(e -> {
            assertThat(e.getGroupName()).isEqualTo("ИС1-33");
            assertThat(e.getSnapshot().getScheduleDate()).isEqualTo(DATE);
            assertThat(e.getSnapshot().getHash()).isEqualTo(hashOf(Map.of("ИС1-33", day("Математика"))));
        });
        verify(scheduleCache).clear();
        verify(scheduleAllCache).clear();
    }

    @Test
    void returnsNoChangesWhenHashIsSame() {
        when(scheduleService.getScheduleForDate(eq("ИС1-33"), eq(DATE), anyList())).thenReturn(day("Математика"));
        ScheduleSnapshot existing = ScheduleSnapshot.builder().id(1L).scheduleDate(DATE)
                .hash(hashOf(Map.of("ИС1-33", day("Математика")))).build();
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.of(existing));

        SnapshotBuiltStatus status = service.buildAndSave(DATE, List.of("ИС1-33"), Map.of());

        assertThat(status).isEqualTo(SnapshotBuiltStatus.NO_CHANGES);
        verify(snapshotRepository, never()).save(any());
        verifyNoInteractions(entryRepository, scheduleCache, scheduleAllCache);
    }

    @Test
    void replacesEntriesWhenHashDiffers() {
        when(scheduleService.getScheduleForDate(eq("ИС1-33"), eq(DATE), anyList())).thenReturn(day("Физика"));
        ScheduleSnapshot existing = ScheduleSnapshot.builder().id(1L).scheduleDate(DATE).hash("old").build();
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.of(existing));

        SnapshotBuiltStatus status = service.buildAndSave(DATE, List.of("ИС1-33"), Map.of());

        assertThat(status).isEqualTo(SnapshotBuiltStatus.UPDATED);
        assertThat(existing.getHash()).isNotEqualTo("old");
        verify(entryRepository).deleteBySnapshot_Id(1L);
        assertThat(savedEntries()).singleElement()
                .satisfies(e -> assertThat(e.getNumeratorSubjects()).containsExactly("Физика"));
    }

    @Test
    void passesChangesToScheduleAndMarksChangedPairs() {
        PairSlot change = new PairSlot(1, new CellData("❗ Химия", "404", "в предмете"), null);
        DaySchedule merged = new DaySchedule("Понедельник");
        merged.addPair(change);
        when(scheduleService.getScheduleForDate("ИС1-33", DATE, List.of(change))).thenReturn(merged);
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.empty());

        service.buildAndSave(DATE, List.of("ИС1-33"), Map.of("ИС1-33", List.of(change)));

        assertThat(savedEntries()).singleElement()
                .satisfies(e -> assertThat(e.isHasChanges()).isTrue());
    }

    @Test
    void groupThatFailsToBuildIsSkipped() {
        when(scheduleService.getScheduleForDate(eq("ИС1-33"), eq(DATE), anyList())).thenReturn(day("Математика"));
        when(scheduleService.getScheduleForDate(eq("СА1-21"), eq(DATE), anyList()))
                .thenThrow(new RuntimeException("битый файл"));
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.empty());

        SnapshotBuiltStatus status = service.buildAndSave(DATE, List.of("ИС1-33", "СА1-21"), Map.of());

        assertThat(status).isEqualTo(SnapshotBuiltStatus.CREATED);
        assertThat(savedEntries()).extracting(ScheduleEntry::getGroupName).containsOnly("ИС1-33");
    }

    @Test
    void existsForDateChecksRepository() {
        when(snapshotRepository.findByScheduleDate(DATE)).thenReturn(Optional.of(new ScheduleSnapshot()));
        when(snapshotRepository.findByScheduleDate(DATE.plusDays(1))).thenReturn(Optional.empty());

        assertThat(service.existsForDate(DATE)).isTrue();
        assertThat(service.existsForDate(DATE.plusDays(1))).isFalse();
    }
}
