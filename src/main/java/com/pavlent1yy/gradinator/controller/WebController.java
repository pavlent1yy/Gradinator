package com.pavlent1yy.gradinator.controller;


import com.pavlent1yy.gradinator.service.ScheduleService;
import com.pavlent1yy.gradinator.service.ScheduleWebParserService;
import com.pavlent1yy.gradinator.service.WeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
//@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class WebController {
    private final ScheduleService scheduleService;
    private final ScheduleWebParserService parserService;
    private final WeekService weekService;

    @GetMapping("/schedule")
    public String getMainPage() {
        return "index";
    }

    @PostMapping("/schedule")
    public String getCurrentSchedule(@RequestParam String group, Model model) {
        model.addAttribute("weekType", weekService.getWeekType());
        model.addAttribute("schedule", scheduleService.getCurrentScheduleWithChanges(group));
        model.addAttribute("group", group);
        return "index";
    }

    @PostMapping("/schedule-no-changes")
    public String getTodayNoChanges(@RequestParam String group, Model model) {
        model.addAttribute("weekType", weekService.getWeekType());
        model.addAttribute("schedule", scheduleService.getTodayWithNoChanges(group));
        model.addAttribute("group", group);
        return "index";
    }

    @PostMapping("/tommorow-schedule-no-changes")
    public String getTomorrowNoChanges(@RequestParam String group, Model model) {
        model.addAttribute("weekType", weekService.getWeekType());
        model.addAttribute("schedule", scheduleService.getTomorrowWithNoChanges(group));
        model.addAttribute("group", group);
        return "index";
    }

}
