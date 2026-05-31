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

    private static final int COL_NUMBER  = 0;
    private static final int COL_SUBJECT = 1;
    private static final int COL_TEACHER = 5;
    private static final int COL_ROOM    = 8;

    private static final Set<String> DAYS = Set.of(
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
    );

    public List<GroupSchedule> scan(Sheet sheet) {
        List<GroupSchedule> result = new ArrayList<>();

        GroupSchedule group = null;
        DaySchedule   day   = null;

        int i = 0;
        int lastRow = sheet.getLastRowNum();

        while (i <= lastRow) {
            Row row = sheet.getRow(i);
            i++;

            if (row == null) continue;

            String c0 = cell(row, COL_NUMBER);

            if (c0.startsWith("*")) continue;

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
                int pairIndex = i - 1;
                PairSlot slot = readPair(sheet, pairIndex);
                day.addPair(slot);

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

        int number = parseIntSafe(cell(main, COL_NUMBER));

        // Числитель — может быть пустым (пара не назначена)
        CellData numerator = readCellData(main);

        CellData denominator = null;
        if (next != null && isContinuation(next)) {
            denominator = readCellData(next);
        }

        return new PairSlot(number, numerator, denominator);
    }

    private boolean hasContinuation(Sheet sheet, int nextIndex) {
        Row next = sheet.getRow(nextIndex);
        return next != null && isContinuation(next);
    }


    private boolean isContinuation(Row row) {
        String c0 = cell(row, COL_NUMBER);
        boolean noNumber = c0.isBlank() || "0.0".equals(c0);
        boolean hasData  = !cell(row, COL_SUBJECT).isBlank()
                || !cell(row, COL_TEACHER).isBlank()
                || !cell(row, COL_ROOM).isBlank();
        return noNumber && hasData;
    }

    private CellData readCellData(Row row) {
        return new CellData(
                normalizeMultiline(cell(row, COL_SUBJECT)),
                normalizeMultiline(cell(row, COL_TEACHER)),
                normalizeMultiline(cell(row, COL_ROOM))
        );
    }

    private String normalizeMultiline(String value) {
        if (value == null) return "";
        return value.replace("\n", " / ").trim();
    }

    private boolean isGroup(String v) {
        return v != null && v.matches(".*[А-ЯA-Z]{2}\\d-\\d{2}.*");
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

    private String cell(Row row, int col) {
        if (row == null) return "";
        var c = row.getCell(col);
        if (c == null) return "";
        return c.toString().trim();
    }
}