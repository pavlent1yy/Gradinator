package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.GroupEntity;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import com.pavlent1yy.gradinator.repository.GroupEntityRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.pavlent1yy.gradinator.service.GroupFileMap.getPossibleFileByGroupPrefix;

@Slf4j
@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;
    private final GroupEntityRepository groupRepository;

    public List<GroupSchedule> getGroupSchedule(String group) {
        String fileName = getPossibleFileByGroupPrefix(group);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("scheduleFiles/" + fileName)) {
            if (is == null) throw new FileNotFoundException(fileName);

            try (Workbook wb = new XSSFWorkbook(is)) {
                List<GroupSchedule> result = new ArrayList<>();
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    result.addAll(scanner.scan(wb.getSheetAt(i)));
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GroupSchedule getWeek(String group) {
        return getGroupSchedule(group).stream()
                .filter(g -> g.getGroup().equals(group))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Расписание группы на конкретную дату, смерженное с уже готовым списком замен на эту дату.
     * Никакого похода в веб тут больше нет — changesForGroup передаётся снаружи (из AllChanges).
     */
    public DaySchedule getScheduleForDate(String group, LocalDate date, List<PairSlot> changesForGroup) {
        int weekDay = getScheduleDayIndex(date);
        DaySchedule schedule = getWeek(group).getDays().get(weekDay);

        Map<Integer, PairSlot> merged = new HashMap<>();
        for (PairSlot pair : schedule.getPairs()) {
            merged.put(pair.getPairNumber(), pair);
        }

        for (PairSlot change : changesForGroup) {
            PairSlot original = merged.get(change.getPairNumber());

            if (isAccordingToSchedule(change) && original != null) {
                change.getNumerator().setSubjects(new ArrayList<>(original.getNumerator().getSubjects()));
                change.getNumerator().setTeachers(new ArrayList<>(original.getNumerator().getTeachers()));
            }

            merged.put(change.getPairNumber(), change);
        }

        List<PairSlot> result = merged.values().stream()
                .sorted(Comparator.comparingInt(PairSlot::getPairNumber))
                .toList();

        DaySchedule copy = new DaySchedule(schedule.getDay());
        copy.setPairs(new ArrayList<>(result));
        return copy;
    }

    public List<String> getAllGroupsFromFiles() {
        log.info("Начинаем поиск всех групп в файлах расписания");

        Set<String> groups = new HashSet<>();

        for (String fileName : GroupFileMap.getAllFiles()) {
            collectGroupsFromFile(fileName, groups);
        }

        log.info("Найдено {} уникальных групп", groups.size());

        return new ArrayList<>(groups);
    }

    private void collectGroupsFromFile(String fileName, Set<String> groups) {
        log.debug("Обрабатываем файл '{}'", fileName);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("scheduleFiles/" + fileName)) {
            if (is == null) {
                log.warn("Файл '{}' не найден в resources", fileName);
                return;
            }

            try (Workbook wb = new XSSFWorkbook(is)) {
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    collectGroupsFromSheet(wb.getSheetAt(i), fileName, groups);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при обработке файла '{}'", fileName, e);
            throw new RuntimeException(e);
        }
    }

    private void collectGroupsFromSheet(Sheet sheet, String fileName, Set<String> groupNames) {
        for (GroupSchedule gs : scanner.scan(sheet)) {
            String groupName = gs.getGroup();

            if (GroupFileMap.getPossibleFileByGroupPrefix(groupName) == null) {
                log.warn(
                        "Группа '{}' найдена в {}, но не сматчилась ни с одним префиксом в GroupFileMap — пропускаем",
                        groupName,
                        fileName
                );
                continue;
            }

            groupNames.add(groupName);
        }
    }

    public void checkGroupSync(Set<String> inputGroups){
        Set<String> dbGroups = groupRepository.findAll().stream()
                .map(GroupEntity::getName)
                .collect(Collectors.toSet());

        Set<String> diff = new HashSet<>(inputGroups);
        diff.removeAll(dbGroups);
        logAboutGroups(diff);
    }

    @Async
    public void checkGroupSync(Set<String> inputGroups, Set<String> dbGroups){
        Set<String> diff = new HashSet<>(inputGroups);
        diff.removeAll(dbGroups);
        logAboutGroups(diff);
    }

    private void logAboutGroups(Set<String> diff){
        if (!diff.isEmpty()){
            log.warn("Найдены группы, не записанные в базу: {}", diff);
        }
    }

    private boolean isAccordingToSchedule(PairSlot pair) {
        return pair != null
                && pair.getNumerator() != null
                && pair.getNumerator().getSubjects().stream()
                .anyMatch(s -> s.toLowerCase().contains("по расписанию"));
    }

    private int getScheduleDayIndex(LocalDate date) {
        int day = date.getDayOfWeek().getValue() - 1;
        return day == 6 ? 0 : day;
    }
}