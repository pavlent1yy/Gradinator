package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.dto.DayScheduleResponse;
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
        LocalDate date = LocalDate.of(2022, 1, 1);

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

//    @Test // TODO: FIX TEST
//    void getSchedule_shouldReturnSchedule_whenGroupExists() {
//        LocalDate date = LocalDate.now();
//
//        DayScheduleResponse schedule = new DayScheduleResponse(
//                "ABC-1",
//                "MONDAY",
//                date,
//                List.of()
//        );
//
//        when(queryService.getScheduleForGroup("ABC-1", date))
//                .thenReturn(Optional.of(schedule));
//
//        ResponseEntity<?> response = controller.getSchedule("ABC-1", date.toString());
//
//        assertThat(response.getStatusCode().value()).isEqualTo(200);
//        assertThat(response.getBody()).isSameAs(schedule);
//
//        verify(queryService).getScheduleForGroup("ABC-1", date);
//    }
}