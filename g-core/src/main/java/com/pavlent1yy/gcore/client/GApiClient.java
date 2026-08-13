package com.pavlent1yy.gcore.client;

import com.pavlent1yy.gcore.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class GApiClient {

    private final RestClient restClient;


    public ScheduleResponse getSchedule(String group, LocalDate date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/schedule")
                        .queryParam("group", group)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .body(ScheduleResponse.class);
    }
}
