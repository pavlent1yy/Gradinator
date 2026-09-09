package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.service.parser.ExcelFileParserService;
import com.pavlent1yy.gradinator.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {

    private final ExcelFileParserService excelFileParserService;
    private final GroupRepository groupRepository;
    private final GroupFileMap groupFileMap;

    public List<String> getAllGroups(){
        return groupRepository.findAll().stream().map(Group::getName).toList();
    }

    public Map<String, List<String>> getGroupsWithDepartments(){
        return groupFileMap.getGroupsByFilePart(getAllGroups());
    }

    @Async
    public void checkForNewGroupsAsync(Set<String> dbGroups) {
        Set<String> fileGroups = excelFileParserService.findAllGroupsInFiles();

        Set<String> missingFromDb = new HashSet<>(fileGroups);
        missingFromDb.removeAll(dbGroups);

        Set<String> staleInDb = new HashSet<>(dbGroups);
        staleInDb.removeAll(fileGroups);

        if (!missingFromDb.isEmpty()) {
            String values = missingFromDb.stream()
                    .map(g -> "('%s', '%s')".formatted(
                            escape(g),
                            escape(groupFileMap.getPossibleFileNameByGroup(g))))
                    .collect(Collectors.joining(",\n"));
            log.warn("🟠 Группы есть в файлах, но не в БД ({} шт):\nINSERT INTO groups (name, xlsx_file) VALUES\n{};",
                    missingFromDb.size(), values);
        }

        if (!staleInDb.isEmpty()) {
            String names = staleInDb.stream()
                    .map(g -> "'" + escape(g) + "'")
                    .collect(Collectors.joining(", "));
            log.warn("🟠 Группы есть в БД, но отсутствуют в файлах этого семестра ({} шт) — вероятно переименованы/расформированы:\nDELETE FROM groups WHERE name IN ({});",
                    staleInDb.size(), names);
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

}
