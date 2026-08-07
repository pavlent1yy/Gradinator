package com.pavlent1yy.gradinator.parser;

import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExcelLayoutScanner {

    private static final int COL_NUMBER  = 0;
    private static final int COL_SUBJECT = 1;
    private static final int COL_TEACHER = 5;
    private static final int COL_ROOM    = 8;

    private enum RowType { GROUP, DAY, PAIR, CONTINUATION, FOOTER, EMPTY }

    private static final Set<String> DAYS = Set.of(
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
    );


    private static final Pattern GROUP_TOKEN =
            Pattern.compile("^[А-ЯЁA-Z]{1,3}\\s?\\d+-\\d{2}");

    private RowType classify(Row row) {
        String c0 = cellString(row, COL_NUMBER);

        if (c0.isBlank()) {
            boolean hasData = !cellString(row, COL_SUBJECT).isBlank()
                    || !cellString(row, COL_TEACHER).isBlank()
                    || !cellString(row, COL_ROOM).isBlank();
            return hasData ? RowType.CONTINUATION : RowType.EMPTY;
        }

        if (c0.startsWith("*")) return RowType.FOOTER;

        if (DAYS.contains(normalizeWhitespace(c0))) return RowType.DAY;

        String firstToken = c0.split("[/\\\\]")[0].trim();
        if (GROUP_TOKEN.matcher(firstToken).find()) return RowType.GROUP;

        if (c0.matches("^\\d+$")) return RowType.PAIR;

        log.debug("🐜Unclassified row, c0='{}' — skipping", c0);
        return RowType.EMPTY;
    }

    public List<GroupSchedule> scan(Sheet sheet) {
        List<Row> rows = collectRows(sheet);

        List<GroupSchedule> result = new ArrayList<>();
        GroupSchedule group = null;
        DaySchedule   day   = null;

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);

            switch (classify(row)) {

                case GROUP -> {
                    group = new GroupSchedule(extractGroup(row));
                    result.add(group);
                    day = null;
                }

                case DAY -> {
                    if (group == null) {
                        log.warn("🟠Day row without preceding group, row {}", i);
                        break;
                    }
                    day = new DaySchedule(normalizeWhitespace(cellString(row, COL_NUMBER)));
                    group.addDay(day);
                }

                case PAIR -> {
                    if (day == null) {
                        log.warn("🟠Pair row without preceding day, row {}", i);
                        break;
                    }
                    int num = Integer.parseInt(cellString(row, COL_NUMBER));
                    CellData numerator = readCellData(row);

                    CellData denominator = null;
                    if (i + 1 < rows.size() && classify(rows.get(i + 1)) == RowType.CONTINUATION) {
                        i++;
                        denominator = readCellData(rows.get(i));
                    }

                    day.addPair(new PairSlot(num, numerator, denominator));
                }

                case CONTINUATION ->
                        log.debug("🐜Orphan continuation at row {}, subject='{}'",
                                i, cellString(row, COL_SUBJECT));

                case FOOTER, EMPTY -> { /* лютый игнор */ }
            }
        }

        return result;
    }


    private List<Row> collectRows(Sheet sheet) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r != null) rows.add(r);
        }
        return rows;
    }


    private String extractGroup(Row row) {
        String raw = cellString(row, COL_NUMBER);
        Matcher m = GROUP_TOKEN.matcher(raw);
        String group = m.find() ? m.group().trim() : raw.trim();
        return group.replaceAll("\\s+", "");
    }


    private CellData readCellData(Row row) {
        return new CellData(
                splitLines(cellString(row, COL_SUBJECT)),
                splitLines(cellString(row, COL_TEACHER)),
                splitLines(cellString(row, COL_ROOM))
        );
    }

    private List<String> splitLines(String value) {
        if (value.isBlank()) return List.of();
        return Arrays.stream(value.split("\r?\n|\r"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String normalizeWhitespace(String s) {
        return s.replaceAll("[\\u00A0\\u200B\\u2007\\u202F\\s]+", " ").trim();
    }

    private String cellString(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            long l = (long) d;
            return d == l ? String.valueOf(l) : String.valueOf(d);
        }
        String raw = cell.toString().trim();
        return col == COL_NUMBER ? normalizeWhitespace(raw) : raw;
    }
}