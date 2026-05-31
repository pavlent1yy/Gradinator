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

        GroupSchedule currentGroup = null;
        DaySchedule currentDay = null;

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            String c0 = get(row, 0);

            System.out.println("ROW0=" + c0 + " | raw=" + (row.getCell(0) != null ? row.getCell(0).toString() : "null"));

            // Группа
            if (isGroupHeader(c0)) {
                currentGroup = new GroupSchedule(extractGroup(c0));
                result.add(currentGroup);
                currentDay = null;
                continue;
            }

            if (currentGroup == null) continue;

            // День
            if (isDayHeader(c0)) {
                currentDay = new DaySchedule(c0.trim());
                currentGroup.addDay(currentDay);
                continue;
            }

            if (currentDay == null) continue;

            // Пара
            if (looksLikeNumber(c0)) {

                PairSlot slot = parsePair(sheet, i);

                currentDay.addPair(slot);

                // если есть знаменатель - пропускаем следующую строку
                if (hasContinuation(sheet, i)) {
                    i++;
                }
            }
        }

        return result;
    }

    private PairSlot parsePair(Sheet sheet, int i) {

        Row main = sheet.getRow(i);
        Row second = sheet.getRow(i + 1);

        int pairNumber = parseIntSafe(get(main, 0));

        CellData numerator = new CellData(
                get(main, 1),
                get(main, 2),
                get(main, 3)
        );

        CellData denominator = null;

        if (second != null && isContinuation(second)) {
            denominator = new CellData(
                    get(second, 1),
                    get(second, 2),
                    get(second, 3)
            );
        }

        return new PairSlot(pairNumber, numerator, denominator);
    }

    private boolean hasContinuation(Sheet sheet, int i) {
        Row next = sheet.getRow(i + 1);
        return next != null && isContinuation(next);
    }

    private boolean isContinuation(Row row) {
        String c0 = get(row, 0);
        String c1 = get(row, 1);
        String c2 = get(row, 2);
        String c3 = get(row, 3);

        boolean emptyFirst = c0 == null || c0.isBlank() || c0.equals("0.0");

        return emptyFirst && (
                !c1.isBlank() || !c2.isBlank() || !c3.isBlank()
        );
    }

    private boolean isGroupHeader(String cell) {
        return cell != null && cell.contains("-") && cell.matches(".*\\d-\\d{2}.*");
    }

    private boolean isDayHeader(String cell) {
        return DAYS.contains(cell);
    }

    private boolean isPairNumber(String cell) {
        if (cell == null || cell.isBlank()) return false;

        try {
            Double.parseDouble(cell.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractGroup(String cell) {
        return cell.split("/")[0].trim();
    }

    private int parseIntSafe(String v) {
        try {
            return (int) Double.parseDouble(v.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean looksLikeNumber(String cell) {
        if (cell == null || cell.isBlank()) return false;

        try {
            double d = Double.parseDouble(cell.trim());
            return d >= 0 && d <= 10; // максимум 7 пар
        } catch (Exception e) {
            return false;
        }
    }

    private String get(Row row, int i) {
        if (row == null || row.getCell(i) == null) return "";
        return row.getCell(i).toString().trim();
    }
}