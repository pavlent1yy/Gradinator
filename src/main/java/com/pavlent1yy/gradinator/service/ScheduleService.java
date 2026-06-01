package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.misc.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pavlent1yy.gradinator.enums.WeekType.NUMERATOR;
import static com.pavlent1yy.gradinator.enums.WeekType.DENOMINATOR;

@Service
@AllArgsConstructor
public class ScheduleService {

    private final ExcelLayoutScanner scanner;
//    private final ScheduleWebParserService parserService;

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
        List<PairSlot> pairs = getWeek(fileName, group).getDays().get(today).getPairs();
        DaySchedule todaySchedule = getWeek(fileName, group).getDays().get(today);
        todaySchedule.setPairs(pairs);
        return todaySchedule;
    }
    

}
