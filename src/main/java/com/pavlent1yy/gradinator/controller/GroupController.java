package com.pavlent1yy.gradinator.controller;
import com.pavlent1yy.gradinator.service.GroupService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<String> getAllGroups() {
        return groupService.getAllGroups().stream().sorted().toList();
    }
}