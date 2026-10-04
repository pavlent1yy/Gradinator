package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.repository.GroupRepository;
import com.pavlent1yy.gradinator.service.parser.ExcelFileParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupSyncServiceTest {

    @Mock
    private ExcelFileParserService excelFileParserService;

    @Mock
    private GroupFileMap groupFileMap;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private GroupSyncService service;

    private static Group group(String name, String file) {
        return Group.builder().name(name).xlsxFile(file).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsNewUpdatesMovedAndRemovesStaleGroups() {
        Group moved = group("СА1-21", "old.xlsx");
        Group untouched = group("ИБ1-11", "oit.xlsx");
        Group stale = group("ЮР1-11", "out.xlsx");

        when(excelFileParserService.findAllGroupsInFiles())
                .thenReturn(Set.of("ИС1-33", "СА1-21", "ИБ1-11", "XX9-99"));
        when(groupRepository.findAll())
                .thenReturn(List.of(moved, untouched, stale))
                .thenReturn(List.of(moved, untouched));
        when(groupFileMap.getPossibleFileNameByGroup("ИС1-33")).thenReturn("oit.xlsx");
        when(groupFileMap.getPossibleFileNameByGroup("СА1-21")).thenReturn("oit.xlsx");
        when(groupFileMap.getPossibleFileNameByGroup("ИБ1-11")).thenReturn("oit.xlsx");
        when(groupFileMap.getPossibleFileNameByGroup("XX9-99")).thenReturn(null);

        List<Group> result = service.syncAndGetGroups();

        ArgumentCaptor<List<Group>> saved = ArgumentCaptor.forClass(List.class);
        verify(groupRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Group::getName).containsExactlyInAnyOrder("ИС1-33", "СА1-21");
        assertThat(moved.getXlsxFile()).isEqualTo("oit.xlsx");

        ArgumentCaptor<Collection<String>> deleted = ArgumentCaptor.forClass(Collection.class);
        verify(groupRepository).deleteAllById(deleted.capture());
        assertThat(deleted.getValue()).containsExactly("ЮР1-11");

        assertThat(result).containsExactly(moved, untouched);
    }

    @Test
    void doesNothingWhenEverythingIsInSync() {
        Group group = group("ИС1-33", "oit.xlsx");
        when(excelFileParserService.findAllGroupsInFiles()).thenReturn(Set.of("ИС1-33"));
        when(groupRepository.findAll()).thenReturn(List.of(group));
        when(groupFileMap.getPossibleFileNameByGroup("ИС1-33")).thenReturn("oit.xlsx");

        assertThat(service.syncAndGetGroups()).containsExactly(group);

        verify(groupRepository, never()).saveAll(any());
        verify(groupRepository, never()).deleteAllById(any());
    }

    @Test
    void syncResultListsCurrentNames() {
        var result = new GroupSyncService.SyncResult(List.of(), List.of(), List.of());

        assertThat(result.currentGroupNames(List.of(group("ИС1-33", "a"), group("СА1-21", "b"))))
                .containsExactly("ИС1-33", "СА1-21");
    }
}
