package com.pavlent1yy.gradinator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupFileMapTest {

    @TempDir
    Path tmp;

    private Path storage;
    private Path config;

    @BeforeEach
    void setUp() throws IOException {
        storage = Files.createDirectory(tmp.resolve("files"));
        config = tmp.resolve("groups.cfg");
    }

    private GroupFileMap create(String cfg, String... files) throws IOException {
        Files.writeString(config, cfg);
        for (String f : files) Files.createFile(storage.resolve(f));
        return new GroupFileMap(storage.toString(), config.toString());
    }

    private GroupFileMap standard() throws IOException {
        return create("""
                # комментарий

                oit: ИС1, СА1
                ort: МРТ, ИКС
                """, "oit_1sem.xlsx", "ORT.xlsx", "readme.txt");
    }

    @Test
    void findsFileByGroupPrefix() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleFileNameByGroup("ИС1-33")).isEqualTo("oit_1sem.xlsx");
        assertThat(map.getPossibleFileNameByGroup("СА1-21")).isEqualTo("oit_1sem.xlsx");
        assertThat(map.getPossibleFileNameByGroup("МРТ-21")).isEqualTo("ORT.xlsx");
    }

    @Test
    void groupLookupIsCaseAndWhitespaceInsensitive() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleFileNameByGroup("  ис1-33 ")).isEqualTo("oit_1sem.xlsx");
    }

    @Test
    void reversedGroupFormatIsSupported() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleFileNameByGroup("21 МРТ")).isEqualTo("ORT.xlsx");
    }

    @Test
    void barePrefixIsSupported() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleFileNameByGroup("ИКС")).isEqualTo("ORT.xlsx");
    }

    @Test
    void unknownOrBlankGroupGivesNull() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleFileNameByGroup("ЮР1-11")).isNull();
        assertThat(map.getPossibleFileNameByGroup(null)).isNull();
        assertThat(map.getPossibleFileNameByGroup("   ")).isNull();
    }

    @Test
    void departmentByGroup() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getPossibleDepartmentByGroup("ИС1-33")).isEqualTo("oit");
        assertThat(map.getPossibleDepartmentByGroup("ИКС")).isEqualTo("ort");
        assertThat(map.getPossibleDepartmentByGroup("ЮР1-11")).isNull();
    }

    @Test
    void departmentNamesAndFiles() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getDepartmentsNames()).containsExactlyInAnyOrder("oit", "ort");
        assertThat(map.getAllFiles()).containsExactlyInAnyOrder("oit_1sem.xlsx", "ORT.xlsx");
    }

    @Test
    void groupsAreDistributedByDepartmentAndSorted() throws IOException {
        GroupFileMap map = standard();

        Map<String, List<String>> result = map.getGroupsByFilePart(
                List.of("СА1-21", "МРТ-21", "ИС1-33", "ЮР1-11", "ИС1-11"));

        assertThat(result.keySet()).containsExactly("oit", "ort");
        assertThat(result.get("oit")).containsExactly("ИС1-11", "ИС1-33", "СА1-21");
        assertThat(result.get("ort")).containsExactly("МРТ-21");
    }

    @Test
    void departmentWithoutGroupsGetsEmptyList() throws IOException {
        GroupFileMap map = standard();

        assertThat(map.getGroupsByFilePart(List.of("ИС1-33")).get("ort")).isEmpty();
    }

    @Test
    void failsWhenConfigMissing() {
        assertThatThrownBy(() -> new GroupFileMap(storage.toString(), tmp.resolve("nope.cfg").toString()))
                .hasMessageContaining("Файл конфигурации не найден");
    }

    @Test
    void failsOnLineWithoutColon() {
        assertThatThrownBy(() -> create("oit ИС1", "oit.xlsx"))
                .hasMessageContaining("Некорректная строка");
    }

    @Test
    void failsOnDepartmentWithoutGroups() {
        assertThatThrownBy(() -> create("oit: , ,", "oit.xlsx"))
                .hasMessageContaining("не указаны группы");
    }

    @Test
    void failsOnDuplicatedDepartment() {
        assertThatThrownBy(() -> create("oit: ИС1\nOIT: СА1", "oit.xlsx"))
                .hasMessageContaining("указано несколько раз");
    }

    @Test
    void failsOnGroupInTwoDepartments() {
        assertThatThrownBy(() -> create("oit: ИС1\nort: ис1", "oit.xlsx", "ort.xlsx"))
                .hasMessageContaining("Группа 'ИС1'");
    }

    @Test
    void failsOnEmptyConfig() {
        assertThatThrownBy(() -> create("# только комментарий\n\n"))
                .hasMessageContaining("не содержит ни одного отделения");
    }

    @Test
    void failsWhenStorageDirMissing() throws IOException {
        Files.writeString(config, "oit: ИС1");

        assertThatThrownBy(() -> new GroupFileMap(tmp.resolve("missing").toString(), config.toString()))
                .hasMessageContaining("Директория с файлами не найдена");
    }

    @Test
    void failsWhenDepartmentFileMissing() {
        assertThatThrownBy(() -> create("oit: ИС1\nort: МРТ", "oit.xlsx"))
                .hasMessageContaining("'ort', не найден");
    }

    @Test
    void failsWhenTwoFilesMatchOneDepartment() {
        assertThatThrownBy(() -> create("oit: ИС1", "oit_1sem.xlsx", "oit_2sem.xlsx"))
                .hasMessageContaining("Найдено несколько файлов для 'oit'");
    }
}
