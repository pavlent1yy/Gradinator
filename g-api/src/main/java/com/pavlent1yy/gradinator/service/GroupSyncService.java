package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.Group;
import com.pavlent1yy.gradinator.service.parser.ExcelFileParserService;
import com.pavlent1yy.gradinator.repository.GroupRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class GroupSyncService {

    private final ExcelFileParserService excelFileParserService;
    private final GroupFileMap groupFileMap;
    private final GroupRepository groupRepository;

    public record SyncResult(List<String> added, List<String> updated, List<String> removed) {
        public List<String> currentGroupNames(List<Group> all) {
            return all.stream().map(Group::getName).toList();
        }
    }

    @Transactional
    public List<Group> syncAndGetGroups() {
        Set<String> fileGroups = excelFileParserService.findAllGroupsInFiles();
        Map<String, Group> dbByName = groupRepository.findAll().stream()
                .collect(HashMap::new, (m, g) -> m.put(g.getName(), g), HashMap::putAll);

        List<Group> toSave = new ArrayList<>();
        List<String> added = new ArrayList<>();
        List<String> updated = new ArrayList<>();

        for (String name : fileGroups) {
            String correctFile = groupFileMap.getPossibleFileNameByGroup(name);
            if (correctFile == null) continue;

            Group existing = dbByName.get(name);
            if (existing == null) {
                toSave.add(Group.builder().name(name).xlsxFile(correctFile).build());
                added.add(name);
            } else if (!correctFile.equals(existing.getXlsxFile())) {
                existing.setXlsxFile(correctFile);
                toSave.add(existing);
                updated.add(name);
            }
        }

        Set<String> stale = new HashSet<>(dbByName.keySet());
        stale.removeAll(fileGroups);

        if (!toSave.isEmpty()) groupRepository.saveAll(toSave);
        if (!stale.isEmpty()) groupRepository.deleteAllById(stale);

        if (!added.isEmpty() || !updated.isEmpty() || !stale.isEmpty()) {
            log.info("🔵 Синхронизация групп: +{} добавлено, ~{} обновлено, -{} удалено",
                    added.size(), updated.size(), stale.size());
            if (!added.isEmpty()) log.debug("   добавлены: {}", added);
            if (!updated.isEmpty()) log.debug("   обновлён xlsx_file у: {}", updated);
            if (!stale.isEmpty()) log.debug("   удалены (не найдены в файлах): {}", stale);
        }

        return groupRepository.findAll();
    }
}