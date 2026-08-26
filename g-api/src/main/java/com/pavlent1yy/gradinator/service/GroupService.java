package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupFileMap groupFileMap;

    public List<String> getAllGroups(){
        return groupRepository.findAll().stream().map(Group::getName).toList();
    }

    public Map<String, List<String>> getGroupsWithDepartments(){
        return groupFileMap.getGroupsByFilePart(getAllGroups());
    }

}
