package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/groups/departments")
    public Map<String, List<String>> getGroupsWithDepartments(){
        return scheduleService.getGroupsWithDepartments();
    }

    @GetMapping("/groups/find-department")
    public String getDepartmentsByGroup(@RequestParam String group){
        return scheduleService.getDepartmentsByGroup(group);
    }
}
