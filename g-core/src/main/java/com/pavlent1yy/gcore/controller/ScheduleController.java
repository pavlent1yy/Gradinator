package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.ScheduleResponse;
import com.pavlent1yy.gcore.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/core/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ScheduleResponse getSchedule(@RequestParam String group, @RequestParam LocalDate date) {
        return scheduleService.getSchedule(group, date);
    }

    @GetMapping("/groups")
    public List<String> getAllGroups(){
        return scheduleService.getAllGroups();
    }
}
