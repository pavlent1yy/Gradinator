package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.service.ScheduleService;
import com.pavlent1yy.gradinator.service.ScheduleWebParserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleWebParserService parserService;

    @PostMapping("/all-schedule/{group}")
    public GroupSchedule getSchedule(@PathVariable String group)  {
       return scheduleService.getWeek(group);
    }

    @PostMapping("/today-schedule/{group}")
    public DaySchedule getTodaySchedule(@PathVariable String group){
        return scheduleService.getToday(group);
    }

    @PostMapping("/actual-schedule/{group}")
    public DaySchedule getActualSchedule(@PathVariable String group){
        return scheduleService.getActualChanges(group);
    }

    @GetMapping("/getChanges/{group}")
    public List<PairSlot> getChanges(@PathVariable String group){
        return parserService.getChanges(group);
    }



}
