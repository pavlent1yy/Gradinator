package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;

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
        int today = LocalDate.now().getDayOfWeek().getValue() - 1;
        if (today == 6) today = 0;
        return getWeek(fileName, group).getDays().get(today);
    }

    public WeekType getWeekType(){
        int weekNumber = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return weekNumber % 2 == 0
                ? WeekType.NUMERATOR
                : WeekType.DENOMINATOR;
    }

}
