package com.pavlent1yy.gradinator.service;


import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;

import java.util.*;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;
    private final ScheduleWebParserService parserService;

    public List<GroupSchedule> getAllGroups(String fileName) {
        try (FileInputStream fis = new FileInputStream(
                String.format("C:/Users/User/Documents/scheduleFiles/%s", fileName));
             Workbook wb = new XSSFWorkbook(fis)) {

            List<GroupSchedule> result = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                result.addAll(scanner.scan(wb.getSheetAt(i)));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GroupSchedule getWeek(String fileName, String group){
        return getAllGroups(fileName).stream().filter(g -> g.getGroup().equals(group)).findFirst().orElseThrow();
    }

    public DaySchedule getToday(String fileName, String group){
        int today = getScheduleDayIndex();
        DaySchedule todaySchedule = getWeek(fileName, group).getDays().get(today);
        List<PairSlot> pairs = todaySchedule.getPairs();
        todaySchedule.setPairs(pairs);
        return todaySchedule;
    }

    public DaySchedule getTomorrowWithChanges(String fileName, String group) {
        int tomorrow = getScheduleDayIndex() + 1;

        DaySchedule tomorrowSchedule = getWeek(fileName, group)
                .getDays()
                .get(tomorrow);

        Map<Integer, PairSlot> merged = new HashMap<>();

        for (PairSlot pair : tomorrowSchedule.getPairs()) {
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

        tomorrowSchedule.setPairs(new ArrayList<>(result));
        System.out.println(tomorrowSchedule);
        return tomorrowSchedule;
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
