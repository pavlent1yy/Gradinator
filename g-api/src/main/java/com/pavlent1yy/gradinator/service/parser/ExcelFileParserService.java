package com.pavlent1yy.gradinator.parser;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.service.GroupFileMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ExcelFileParserService {

    private final ExcelLayoutScanner scanner;
    private final StorageContext storageContext;
    private final GroupFileMap groupFileMap;

    public Set<String> findAllGroupsInFiles() {
        Set<String> groups = new HashSet<>();
        for (String fileName : groupFileMap.getAllFiles()) {
            collectGroupsFromFile(fileName, groups);
        }
        return groups;
    }

    private void collectGroupsFromFile(String fileName, Set<String> groups) {
        Path pathToFile = Path.of(storageContext.getStorageDirPath(), fileName);

        try (InputStream is = Files.newInputStream(pathToFile);
             Workbook wb = new XSSFWorkbook(is)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                collectGroupsFromSheet(wb.getSheetAt(i), fileName, groups);
            }
        } catch (IOException e) {
            log.error("⭕ Ошибка при обработке файла '{}'", fileName, e);
            throw new RuntimeException(e);
        }
    }

    private void collectGroupsFromSheet(Sheet sheet, String fileName, Set<String> groupNames) {
        for (GroupSchedule gs : scanner.scan(sheet)) {
            String groupName = gs.getGroup();
            if (groupFileMap.getPossibleFileNameByGroup(groupName) == null) {
                log.warn("🟠 Группа '{}' найдена в {}, но не сматчилась ни с одним префиксом в GroupFileMap — пропускаем",
                        groupName, fileName);
                continue;
            }
            groupNames.add(groupName);
        }
    }
}