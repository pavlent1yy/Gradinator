package com.pavlent1yy.gradinator.parser;

import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExcelLayoutScanner {

    private static final Set<String> DAYS = Set.of(
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
    );

    public List<GroupSchedule> scan(Sheet sheet) {

        List<GroupSchedule> result = new ArrayList<>();

        GroupSchedule group = null;
        DaySchedule day = null;

        int i = 0;

        while (i <= sheet.getLastRowNum()) {

            Row row = sheet.getRow(i);
            i++;

            if (row == null) continue;

            String c0 = cell(row, 0);

            if (isGroup(c0)) {
                group = new GroupSchedule(extractGroup(c0));
                result.add(group);
                day = null;
                continue;
            }

            if (isDay(c0)) {
                if (group == null) continue;

                day = new DaySchedule(c0.trim());
                group.addDay(day);
                continue;
            }

            if (day == null || group == null) continue;

            if (isPairStart(c0)) {
                PairSlot slot = readPair(sheet, i - 1);
                day.addPair(slot);

                // съедаем следующую строку
                if (hasContinuation(sheet, i)) {
                    i++;
                }
            }
        }

        return result;
    }

    private PairSlot readPair(Sheet sheet, int index) {

        Row main = sheet.getRow(index);
        Row next = sheet.getRow(index + 1);

        int number = parseIntSafe(cell(main, 0));

        CellData numerator = readCellData(main);

        CellData denominator = null;

        if (next != null && isContinuation(next)) {
            denominator = readCellData(next);
        }

        return new PairSlot(number, numerator, denominator);
    }

    private boolean hasContinuation(Sheet sheet, int i) {
        Row next = sheet.getRow(i);
        return next != null && isContinuation(next);
    }

    private boolean isContinuation(Row row) {
        String c0 = cell(row, 0);
        return (c0.isBlank() || "0.0".equals(c0)) &&
                (!cell(row, 1).isBlank()
                        || !cell(row, 2).isBlank()
                        || !cell(row, 3).isBlank());
    }

    private CellData readCellData(Row row) {
        return new CellData(
                cell(row, 1),
                cell(row, 2),
                cell(row, 3)
        );
    }

    private boolean isGroup(String v) {
        return v != null && v.matches(".*[А-Я]{2}\\d-\\d{2}.*");
    }

    private boolean isDay(String v) {
        return v != null && DAYS.contains(v.trim());
    }

    private boolean isPairStart(String v) {
        if (v == null || v.isBlank()) return false;
        return v.matches("\\d+(\\.0)?");
    }

    private String extractGroup(String v) {
        return v.split("/")[0].trim();
    }

    private int parseIntSafe(String v) {
        try {
            return (int) Double.parseDouble(v);
        } catch (Exception e) {
            return -1;
        }
    }

    private String cell(Row row, int i) {
        if (row == null || row.getCell(i) == null) return "";
        return row.getCell(i).toString().trim();
    }
}