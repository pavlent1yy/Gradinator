package com.pavlent1yy.gradinator.service;


import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import java.util.*;

import static com.pavlent1yy.gradinator.service.GroupFileMap.getPossibleFileByGroupPrefix;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;
    private final ScheduleWebParserService parserService;

    public List<GroupSchedule> getGroupSchedule(String group) {
        String fileName = getPossibleFileByGroupPrefix(group);
        try (
                InputStream is = getClass()
                        .getClassLoader()
                        .getResourceAsStream("scheduleFiles/" + fileName)
        ) {

            if (is == null) {
                throw new FileNotFoundException(fileName);
            }

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

    public GroupSchedule getWeek(String group){
        return getGroupSchedule(group).stream().filter(g -> g.getGroup().equals(group)).findFirst().orElseThrow();
    }

    public DaySchedule getTodayWithNoChanges(String group){
        DaySchedule todaySchedule = getWeek(group).getDays().get(getScheduleDayIndex());
        todaySchedule.setPairs(todaySchedule.getPairs());
        return todaySchedule;
    }

    public DaySchedule getTomorrowWithNoChanges(String group){
        DaySchedule todaySchedule = getWeek(group).getDays().get(getScheduleDayIndex() + 1);
        todaySchedule.setPairs(todaySchedule.getPairs());
        return todaySchedule;
    }

    public DaySchedule getCurrentScheduleWithChanges(String group) {
        int today = getScheduleDayIndex();

        DaySchedule schedule = getWeek(group)
                .getDays()
                .get(today);

        Map<Integer, PairSlot> merged = new HashMap<>();

        for (PairSlot pair : schedule.getPairs()) {
            merged.put(pair.getPairNumber(), pair);
        }

        for (PairSlot change : parserService.getChanges(group)) {

            PairSlot original = merged.get(change.getPairNumber());

            if (isAccordingToSchedule(change) && original != null) {

                change.getNumerator().setSubjects(
                        new ArrayList<>(original.getNumerator().getSubjects())
                );

                change.getNumerator().setTeachers(
                        new ArrayList<>(original.getNumerator().getTeachers())
                );
            }

            merged.put(change.getPairNumber(), change);
        }

        List<PairSlot> result = merged.values()
                .stream()
                .sorted(Comparator.comparingInt(PairSlot::getPairNumber))
                .toList();

        schedule.setPairs(new ArrayList<>(result));
        return schedule;
    }

    private boolean isAccordingToSchedule(PairSlot pair) {
        return pair != null
                && pair.getNumerator() != null
                && pair.getNumerator().getSubjects().stream()
                .anyMatch(s -> s.toLowerCase().contains("по расписанию"));
    }

    private int getScheduleDayIndex(){
        int today = LocalDate.now().getDayOfWeek().getValue() - 1;
        if (today == 6) today = 0;
        return today;
    }


}
