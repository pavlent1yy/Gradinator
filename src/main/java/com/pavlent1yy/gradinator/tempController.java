package com.pavlent1yy.gradinator;

import com.pavlent1yy.gradinator.service.ScheduleService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class tempController {

    private final ScheduleService scheduleService;

    @GetMapping("/user/{id}")
    public String getSchedule(@PathVariable Long id) {
        return scheduleService.getSchedule(id);
    }
}




