package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.dto.records.FreeRoomsResponse;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.dto.records.SearchHit;
import com.pavlent1yy.gcore.dto.records.WeekDayResponse;
import com.pavlent1yy.gcore.enums.SearchType;
import com.pavlent1yy.gcore.service.ScheduleService;
import com.pavlent1yy.gcore.service.SearchService;
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
    private final SearchService searchService;

    @GetMapping
    public ScheduleResponse getSchedule(@RequestParam String group, @RequestParam LocalDate date) {
        return scheduleService.getSchedule(group, date);
    }

    @GetMapping("/week")
    public List<WeekDayResponse> getWeek(@RequestParam String group, @RequestParam LocalDate date) {
        return scheduleService.getWeek(group, date);
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

    @GetMapping("/groups/department-names")
    public List<String> getDepartmentNames(){
        return scheduleService.getDepartmentNames();
    }

    @GetMapping("/current-weektype")
    public Map<String, String> getCurrentWeekType() {
        return scheduleService.getCurrentWeekType();
    }

    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam String q,
                                  @RequestParam(defaultValue = "ANY") SearchType type,
                                  @RequestParam LocalDate date) {
        return searchService.search(q, type, date);
    }

    @GetMapping("/free-rooms")
    public List<FreeRoomsResponse> getFreeRooms(@RequestParam LocalDate date) {
        return searchService.freeRooms(date);
    }

    @GetMapping("/teachers")
    public List<String> getTeachers() {
        return searchService.teachers();
    }

    @GetMapping("/subjects")
    public List<String> getSubjects() {
        return searchService.subjects();
    }

    @GetMapping("/rooms")
    public List<String> getRooms() {
        return searchService.rooms();
    }
}
