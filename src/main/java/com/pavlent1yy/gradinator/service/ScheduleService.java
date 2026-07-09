package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

import static com.pavlent1yy.gradinator.service.GroupFileMap.getPossibleFileByGroupPrefix;

@Slf4j
@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;

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

    public List<String> getAllGroups() {
        Set<String> groups = new HashSet<>();
        for (String fileName : GroupFileMap.getAllFiles()) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("scheduleFiles/" + fileName)) {
                if (is == null) continue;
                try (Workbook wb = new XSSFWorkbook(is)) {
                    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                        for (GroupSchedule gs : scanner.scan(wb.getSheetAt(i))) {
                            String group = gs.getGroup();
                            if (GroupFileMap.getPossibleFileByGroupPrefix(group) == null) {
                                log.warn("Группа '{}' найдена в {}, но не сматчилась ни с одним префиксом в GroupFileMap — пропускаю", group, fileName);
                                continue;
                            }
                            groups.add(group);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>(groups);
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