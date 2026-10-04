package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.client.GApiClient;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.enums.WeekType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private GApiClient gApiClient;

    @InjectMocks
    private ScheduleService service;

    @Test
    void delegatesEverythingToGApi() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        ScheduleResponse schedule = new ScheduleResponse("ИС1-33", "Понедельник", WeekType.NUMERATOR, date, List.of());
        when(gApiClient.getSchedule("ИС1-33", date)).thenReturn(schedule);
        when(gApiClient.getAllGroups()).thenReturn(List.of("ИС1-33"));
        when(gApiClient.getAllGroupsWithDepartments()).thenReturn(Map.of("oit", List.of("ИС1-33")));
        when(gApiClient.getDepartmentsByGroup("ИС1-33")).thenReturn("oit");
        when(gApiClient.getDepartmentNames()).thenReturn(List.of("oit"));

        assertThat(service.getSchedule("ИС1-33", date)).isSameAs(schedule);
        assertThat(service.getAllGroups()).containsExactly("ИС1-33");
        assertThat(service.getGroupsWithDepartments()).containsEntry("oit", List.of("ИС1-33"));
        assertThat(service.getDepartmentsByGroup("ИС1-33")).isEqualTo("oit");
        assertThat(service.getDepartmentNames()).containsExactly("oit");
    }
}
