package com.pavlent1yy.gcore.client;

import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.dto.records.WeekDayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GApiClient {

    private final RestClient gApiRestClient;

    public ScheduleResponse getSchedule(String group, LocalDate date) {
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/schedule")
                        .queryParam("group", group)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            try {
                                throw new ScheduleNotFoundException(
                                        "Нет актуальных данных для группы '" + group + "' на " + date
                                );
                            } catch (ScheduleNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .body(ScheduleResponse.class);
    }

    public List<String> getAllGroups(){
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/groups").build()
                ).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, List<String>> getAllGroupsWithDepartments(){
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/groups/departments").build()
                ).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public String getDepartmentsByGroup(String group){
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/groups/find-department")
                        .queryParam("group", group).build()
                ).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<String> getDepartmentNames(){
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/groups/department-names").build()
                ).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, ScheduleResponse> getAllSchedules(LocalDate date) {
        try {
            Map<String, ScheduleResponse> schedules = gApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/schedule")
                            .queryParam("date", date)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, ScheduleResponse>>() {});

            return schedules == null ? Map.of() : schedules;
        } catch (HttpClientErrorException.NotFound e) {
            return Map.of();
        }
    }

    public List<WeekDayResponse> getWeek(String group, LocalDate date) {
        List<WeekDayResponse> week = gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/schedule/week")
                        .queryParam("group", group)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return week == null ? List.of() : week;
    }

    public List<String> getTeachers() {
        return getList("/api/teachers");
    }

    public List<String> getSubjects() {
        return getList("/api/subjects");
    }

    public List<String> getRooms() {
        return getList("/api/rooms");
    }

    public Map<String, String> getCurrentWeekType() {
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/schedule/current-weektype").build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private List<String> getList(String path) {
        List<String> values = gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return values == null ? List.of() : values;
    }
}
