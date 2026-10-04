package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.dto.DayScheduleResponse;
import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.service.QueryService;
import com.pavlent1yy.gradinator.service.WeekService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    @Mock
    private QueryService queryService;

    @Mock
    private WeekService weekService;

    @InjectMocks
    private ScheduleController controller;

    @Test
    void getSchedule_shouldReturnBadRequest_whenDateIsInvalid() {
        ResponseEntity<?> response = controller.getSchedule(null, "not-a-date");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody())
                .isInstanceOf(Map.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKey("error");

        verifyNoInteractions(queryService, weekService);
    }

    @Test
    void getSchedule_shouldReturnNotFound_whenSnapshotDoesNotExist() {
        LocalDate date = LocalDate.now().minusDays(1);

        when(queryService.getScheduleForAllGroups(date))
                .thenReturn(Map.of());

        ResponseEntity<?> response = controller.getSchedule(null, date.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody())
                .isEqualTo(Map.of(
                        "error",
                        "Снапшот на дату " + date + " ещё не посчитан"
                ));

        verify(queryService).getScheduleForAllGroups(date);
    }

    @Test
    void getSchedule_shouldReturnOkWithEmptyMap_whenAllGroupsOnDayOff() {
        LocalDate sunday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        when(queryService.getScheduleForAllGroups(sunday)).thenReturn(Map.of());
        when(weekService.isDayOff(sunday)).thenReturn(true);

        ResponseEntity<?> response = controller.getSchedule(null, sunday.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of());
    }

    @Test
    void getSchedule_shouldReturnSchedule_whenGroupExists() {
        LocalDate date = LocalDate.now();

        DayScheduleResponse schedule = new DayScheduleResponse(
                "ABC-1",
                "MONDAY",
                WeekType.NUMERATOR,
                date,
                List.of()
        );

        when(queryService.getScheduleForGroup("ABC-1", date))
                .thenReturn(Optional.of(schedule));

        ResponseEntity<?> response = controller.getSchedule("  ABC-1 ", date.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(schedule);

        verify(queryService).getScheduleForGroup("ABC-1", date);
    }

    @Test
    void getSchedule_shouldReturnNotFound_whenGroupHasNoData() {
        LocalDate date = LocalDate.now();
        when(queryService.getScheduleForGroup("ABC-1", date)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getSchedule("ABC-1", date.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "error", "Нет актуальных данных для группы 'ABC-1' на " + date));
    }

    @Test
    void getSchedule_shouldRejectTooLongGroup() {
        ResponseEntity<?> response = controller.getSchedule("x".repeat(31), null);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(queryService);
    }

    @Test
    void getSchedule_shouldRejectDatesOutsideAllowedRange() {
        assertThat(controller.getSchedule("ABC-1", LocalDate.now().minusDays(91).toString())
                .getStatusCode().value()).isEqualTo(400);
        assertThat(controller.getSchedule("ABC-1", LocalDate.now().plusDays(31).toString())
                .getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(queryService);
    }

    @Test
    void getSchedule_shouldAcceptRangeBorders() {
        when(queryService.getScheduleForGroup(eq("ABC-1"), any()))
                .thenReturn(Optional.empty());

        controller.getSchedule("ABC-1", LocalDate.now().minusDays(90).toString());
        controller.getSchedule("ABC-1", LocalDate.now().plusDays(30).toString());

        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().minusDays(90));
        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().plusDays(30));
    }

    @Test
    void getSchedule_shouldResolveRelativeDateKeywords() {
        when(queryService.getScheduleForGroup(eq("ABC-1"), any()))
                .thenReturn(Optional.empty());

        controller.getSchedule("ABC-1", "today");
        controller.getSchedule("ABC-1", "tomorrow");
        controller.getSchedule("ABC-1", "yesterday");
        controller.getSchedule("ABC-1", null);

        verify(queryService, times(2)).getScheduleForGroup("ABC-1", LocalDate.now());
        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().plusDays(1));
        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().minusDays(1));
    }

    @Test
    void getSchedule_shouldReturnAllGroups_whenGroupBlank() {
        LocalDate date = LocalDate.now();
        Map<String, DayScheduleResponse> all = Map.of("ABC-1",
                new DayScheduleResponse("ABC-1", "Среда", WeekType.NUMERATOR, date, List.of()));
        when(queryService.getScheduleForAllGroups(date)).thenReturn(all);

        ResponseEntity<?> response = controller.getSchedule("  ", date.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(all);
    }

    @Test
    void relativeEndpoints_shouldUseMatchingDates() {
        when(queryService.getScheduleForGroup(eq("ABC-1"), any()))
                .thenReturn(Optional.empty());

        controller.getToday("ABC-1");
        controller.getTomorrow("ABC-1");
        controller.getYesterday("ABC-1");

        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now());
        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().plusDays(1));
        verify(queryService).getScheduleForGroup("ABC-1", LocalDate.now().minusDays(1));
    }

    @Test
    void currentWeekType_shouldReturnTypeAndRussianLabel() {
        when(weekService.getCurrentWeekType()).thenReturn(WeekType.NUMERATOR);
        assertThat(controller.getCurrentWeekType().getBody())
                .isEqualTo(Map.of("weekType", WeekType.NUMERATOR, "label", "Числитель"));

        when(weekService.getCurrentWeekType()).thenReturn(WeekType.DENOMINATOR);
        assertThat(controller.getCurrentWeekType().getBody())
                .isEqualTo(Map.of("weekType", WeekType.DENOMINATOR, "label", "Знаменатель"));
    }
}
