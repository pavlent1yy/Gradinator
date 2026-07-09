package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.service.ScheduleService;
import com.pavlent1yy.gradinator.service.ScheduleWebParserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleWebParserService parserService;

    @PostMapping("/current-schedule/{group}")
    public DaySchedule getCurrentSchedule(@PathVariable String group){
        return scheduleService.getCurrentSchedule(group);
    }




}
