package com.pavlent1yy.gcore.client;

import com.pavlent1yy.gcore.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

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
                .body(ScheduleResponse.class);
    }

    public List<String> getAllGroups(){
        return gApiRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/groups").build()
                ).retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
