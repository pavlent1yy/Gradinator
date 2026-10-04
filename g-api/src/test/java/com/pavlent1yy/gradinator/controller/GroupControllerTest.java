package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.service.GroupFileMap;
import com.pavlent1yy.gradinator.service.GroupService;
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
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupFileMap groupFileMap;

    @InjectMocks
    private GroupController controller;

    @Test
    void groupsAreSorted() {
        when(groupService.getAllGroups()).thenReturn(List.of("СА1-21", "ИС1-33", "ИБ1-11"));

        assertThat(controller.getAllGroups()).containsExactly("ИБ1-11", "ИС1-33", "СА1-21");
    }

    @Test
    void groupsWithDepartmentsAreDelegated() {
        when(groupService.getGroupsWithDepartments()).thenReturn(Map.of("oit", List.of("ИС1-33")));

        assertThat(controller.getGroupsWithDepartments()).containsEntry("oit", List.of("ИС1-33"));
    }

    @Test
    void departmentIsSearchedByPrefix() {
        when(groupFileMap.getPossibleDepartmentByGroup("ИС1")).thenReturn("oit");

        assertThat(controller.getDepartmentByGroup("ИС1-33")).isEqualTo("oit");
    }

    @Test
    void departmentNamesAreDelegated() {
        when(groupFileMap.getDepartmentsNames()).thenReturn(List.of("oit", "ort"));

        assertThat(controller.getDepartmentNames()).containsExactly("oit", "ort");
    }
}
