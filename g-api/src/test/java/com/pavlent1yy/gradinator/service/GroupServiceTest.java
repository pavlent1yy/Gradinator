package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupFileMap groupFileMap;

    @InjectMocks
    private GroupService service;

    @Test
    void returnsGroupNames() {
        when(groupRepository.findAll()).thenReturn(List.of(
                Group.builder().name("СА1-21").build(),
                Group.builder().name("ИС1-33").build()));

        assertThat(service.getAllGroups()).containsExactly("СА1-21", "ИС1-33");
    }

    @Test
    void groupsByDepartmentUseFileMap() {
        when(groupRepository.findAll()).thenReturn(List.of(Group.builder().name("ИС1-33").build()));
        when(groupFileMap.getGroupsByFilePart(List.of("ИС1-33"))).thenReturn(Map.of("oit", List.of("ИС1-33")));

        assertThat(service.getGroupsWithDepartments()).isEqualTo(Map.of("oit", List.of("ИС1-33")));
    }
}
