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
public class TestController {

    private final ScheduleService scheduleService;
    private final ScheduleWebParserService parserService;

    @PostMapping("/parse/{fileName}")
    public List<GroupSchedule> parse(@PathVariable String fileName) {
        return scheduleService.getAllGroups(fileName);
    }


    @PostMapping("/schedule/{fileName}/{group}")
    public GroupSchedule getSchedule(@PathVariable String fileName,
                                     @PathVariable String group)  {
       return scheduleService.getWeek(fileName, group);
    }

    @PostMapping("/today-schedule/{fileName}/{group}")
    public DaySchedule getDaySchedule (@PathVariable String fileName,
                                       @PathVariable String group){
        return scheduleService.getToday(fileName, group);
    }

    @PostMapping("/getChanges/{group}")
    public List<PairSlot> getChanges(@PathVariable String group){
        return parserService.getChanges(group);
    }



}
