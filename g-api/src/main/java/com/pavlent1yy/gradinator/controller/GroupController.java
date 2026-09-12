package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.service.GroupFileMap;
import com.pavlent1yy.gradinator.service.GroupService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupFileMap groupFileMap;

    @GetMapping("")
    public List<String> getAllGroups() {
        return groupService.getAllGroups().stream().sorted().toList();
    }

    @GetMapping("/departments")
    public Map<String, List<String>> getGroupsWithDepartments() {
        return groupService.getGroupsWithDepartments();
    }

    @GetMapping("/find-department")
    public String getDepartmentByGroup(@RequestParam String group){
        return groupFileMap.getPossibleDepartmentByGroup(group.split("-")[0]);
    }

    @GetMapping("/department-names")
    public List<String> getDepartmentNames(){
        return groupFileMap.getDepartmentsNames();
    }
}