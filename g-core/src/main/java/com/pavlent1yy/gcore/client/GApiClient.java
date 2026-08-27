package com.pavlent1yy.gcore.client;

import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
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

}
