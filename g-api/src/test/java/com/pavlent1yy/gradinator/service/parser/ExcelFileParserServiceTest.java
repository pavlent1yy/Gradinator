package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.service.GroupFileMap;
import com.pavlent1yy.gradinator.support.SheetBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcelFileParserServiceTest {

    @TempDir
    Path storageDir;

    private final GroupFileMap groupFileMap = mock(GroupFileMap.class);
    private ExcelFileParserService service;

    @BeforeEach
    void setUp() {
        StorageContext storage = new StorageContext();
        ReflectionTestUtils.setField(storage, "storageDirPath", storageDir.toString());
        storage.init();
        service = new ExcelFileParserService(new ExcelLayoutScanner(), storage, groupFileMap);
        when(groupFileMap.getPossibleFileNameByGroup(anyString())).thenReturn("some.xlsx");
    }

    @Test
    void collectsGroupsFromAllFilesAndSheets() throws Exception {
        new SheetBuilder().group("ИС1-33").newSheet().group("СА1-21").writeTo(storageDir.resolve("oit.xlsx"));
        new SheetBuilder().group("ЮР1-11").group("ИС1-33").writeTo(storageDir.resolve("out.xlsx"));
        when(groupFileMap.getAllFiles()).thenReturn(new LinkedHashSet<>(List.of("oit.xlsx", "out.xlsx")));

        assertThat(service.findAllGroupsInFiles()).containsExactlyInAnyOrder("ИС1-33", "СА1-21", "ЮР1-11");
    }

    @Test
    void skipsGroupsWithoutConfiguredPrefix() throws Exception {
        new SheetBuilder().group("ИС1-33").group("XX9-99").writeTo(storageDir.resolve("oit.xlsx"));
        when(groupFileMap.getAllFiles()).thenReturn(new LinkedHashSet<>(List.of("oit.xlsx")));
        when(groupFileMap.getPossibleFileNameByGroup("XX9-99")).thenReturn(null);

        assertThat(service.findAllGroupsInFiles()).containsExactly("ИС1-33");
    }

    @Test
    void brokenFileFailsTheWholeSearch() throws Exception {
        Files.writeString(storageDir.resolve("oit.xlsx"), "не xlsx");
        when(groupFileMap.getAllFiles()).thenReturn(new LinkedHashSet<>(List.of("oit.xlsx")));

        assertThatThrownBy(() -> service.findAllGroupsInFiles()).isInstanceOf(RuntimeException.class);
    }
}
