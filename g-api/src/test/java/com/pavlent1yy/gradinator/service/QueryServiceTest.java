package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.repository.ScheduleEntryRepository;
import com.pavlent1yy.gradinator.repository.ScheduleSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private ScheduleSnapshotRepository snapshotRepository;

    @Mock
    private ScheduleEntryRepository entryRepository;

    @InjectMocks
    private QueryService service;


    @Test
    void getScheduleForGroup_shouldReturnEmpty_whenSnapshotNotFound() {
        LocalDate date = LocalDate.of(2026, 7, 29);

        when(snapshotRepository.findByScheduleDate(date))
                .thenReturn(Optional.empty());

        Optional<?> result = service.getScheduleForGroup("IS1-33", date);

        assertThat(result).isEmpty();

        verify(snapshotRepository).findByScheduleDate(date);
        verifyNoInteractions(entryRepository);
    }


    @Test
    void getScheduleForGroup_shouldReturnEmpty_whenGroupHasNoEntries() {
        LocalDate date = LocalDate.of(2026, 7, 29);

        ScheduleSnapshot snapshot = new ScheduleSnapshot();
        snapshot.setId(1L);

        when(snapshotRepository.findByScheduleDate(date))
                .thenReturn(Optional.of(snapshot));

        when(entryRepository.findBySnapshot_Id(1L))
                .thenReturn(List.of());

        Optional<?> result = service.getScheduleForGroup("IS1-33", date);

        assertThat(result).isEmpty();

        verify(entryRepository).findBySnapshot_Id(1L);
    }

    @Test
    void getScheduleForGroup_shouldReturnSortedPairs_whenGroupExists() {
        LocalDate date = LocalDate.of(2026, 7, 29);

        ScheduleSnapshot snapshot = ScheduleSnapshot.builder()
                .id(1L)
                .scheduleDate(date)
                .build();

        ScheduleEntry secondPair = ScheduleEntry.builder()
                .groupName("IS1-33")
                .day("Среда")
                .pairNumber(3)
                .hasChanges(true)
                .numeratorSubjects(List.of("Физика"))
                .numeratorTeachers(List.of("Иванов"))
                .numeratorRooms(List.of("101"))
                .denominatorSubjects(List.of())
                .denominatorTeachers(List.of())
                .denominatorRooms(List.of())
                .build();

        ScheduleEntry firstPair = ScheduleEntry.builder()
                .groupName("IS1-33")
                .day("Среда")
                .pairNumber(1)
                .hasChanges(false)
                .numeratorSubjects(List.of("Математика"))
                .numeratorTeachers(List.of("Петров"))
                .numeratorRooms(List.of("202"))
                .denominatorSubjects(List.of())
                .denominatorTeachers(List.of())
                .denominatorRooms(List.of())
                .build();


        when(snapshotRepository.findByScheduleDate(date))
                .thenReturn(Optional.of(snapshot));

        when(entryRepository.findBySnapshot_Id(1L))
                .thenReturn(List.of(secondPair, firstPair));


        var result = service.getScheduleForGroup("IS1-33", date);


        assertThat(result).isPresent();

        var response = result.get();

        assertThat(response.group())
                .isEqualTo("IS1-33");

        assertThat(response.date())
                .isEqualTo(date);

        assertThat(response.day())
                .isEqualTo("Среда");


        assertThat(response.pairs())
                .hasSize(2);

        // Проверяем сортировку
        assertThat(response.pairs().get(0).pairNumber())
                .isEqualTo(1);

        assertThat(response.pairs().get(1).pairNumber())
                .isEqualTo(3);


        // Проверяем данные пары
        var first = response.pairs().get(0);

        assertThat(first.numerator())
                .isNotNull();

        assertThat(first.numerator().getSubjects())
                .containsExactly("Математика");

        assertThat(first.numerator().getTeachers())
                .containsExactly("Петров");

        assertThat(first.hasChanges())
                .isFalse();


        var second = response.pairs().get(1);

        assertThat(second.hasChanges())
                .isTrue();


        verify(snapshotRepository)
                .findByScheduleDate(date);

        verify(entryRepository)
                .findBySnapshot_Id(1L);
    }
}