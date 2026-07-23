package com.pavlent1yy.gradinator.controller;
import com.pavlent1yy.gradinator.service.ScheduleService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class GroupController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<String> getAllGroups() {
        return scheduleService.getAllGroups().stream().sorted().toList();
    }
}