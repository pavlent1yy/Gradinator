package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.client.GApiClient;
import com.pavlent1yy.gcore.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final GApiClient gApiClient;

    public ScheduleResponse getSchedule(String group, LocalDate date) {
        return gApiClient.getSchedule(group, date);
    }

    public List<String> getAllGroups(){
        return gApiClient.getAllGroups();
    }
}
