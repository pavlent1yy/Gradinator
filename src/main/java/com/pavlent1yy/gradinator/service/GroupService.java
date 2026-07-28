package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.GroupEntity;
import com.pavlent1yy.gradinator.repository.GroupEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupEntityRepository groupRepository;

    public List<String> getAllGroups(){
        return groupRepository.findAll().stream().map(GroupEntity::getName).toList();
    }

}
