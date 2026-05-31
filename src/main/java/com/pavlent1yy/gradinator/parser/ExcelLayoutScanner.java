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
import java.util.regex.Pattern;

@Slf4j
@Component
public class ExcelLayoutScanner {

    // Реальные индексы колонок в файлах
    private static final int COL_NUMBER  = 0;
    private static final int COL_SUBJECT = 1;
    private static final int COL_TEACHER = 5;
    private static final int COL_ROOM    = 8;

    private static final Set<String> DAYS = Set.of(
            "Понедельник", "Вторник", "Среда",
            "Четверг", "Пятница", "Суббота"
    );

    // Минимум: 2 кириллических буквы + цифра + дефис + 2 цифры.
    // Учитываем пробелы внутри кода группы ("ЮР 1-11") и разные разделители (/ \ пробел)
    private static final Pattern GROUP_PATTERN =
            Pattern.compile("[А-ЯЁа-яёA-Za-z]{2}[\\s]?\\d+-\\d{2}");

    // Номер пары: целое или float-строка ("0", "1", "2.0" и т.д.)
    private static final Pattern PAIR_NUMBER_PATTERN =
            Pattern.compile("^\\d+(\\.0+)?$");

    public List<GroupSchedule> scan(Sheet sheet) {
        List<GroupSchedule> result = new ArrayList<>();

        GroupSchedule group = null;
        DaySchedule   day   = null;

        List<Row> rows = collectRows(sheet);
        int size = rows.size();
        int i = 0;

        while (i < size) {
            Row row = rows.get(i);
            i++;

            String c0 = cellString(row, COL_NUMBER);

            if (c0.startsWith("*")) continue; // служебная заметка

            if (isGroup(c0)) {
                group = new GroupSchedule(extractGroup(c0));
                result.add(group);
                day = null;
                log.debug("Group: {}", group.getGroup());
                continue;
            }

            if (isDay(c0)) {
                if (group == null) {
                    log.warn("Day '{}' without group at row ~{}", c0, i);
                    continue;
                }
                day = new DaySchedule(c0.trim());
                group.addDay(day);
                continue;
            }

            if (day == null || group == null) continue;

            if (isPairNumber(c0)) {
                int pairNum = parsePairNumber(c0);
                CellData numerator = readCellData(row);

                // Смотрим вперёд: следующая строка — знаменатель?
                CellData denominator = null;
                if (i < size && isContinuation(rows.get(i))) {
                    denominator = readCellData(rows.get(i));
                    i++; // потребляем строку знаменателя
                }

                day.addPair(new PairSlot(pairNum, numerator, denominator));
            }
        }

        return result;
    }

    /**
     * Собираем все строки листа в список заранее — избавляемся от
     * хрупкого ручного управления индексом и null-провалов getRow().
     */
    private List<Row> collectRows(Sheet sheet) {
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            rows.add(r != null ? r : null); // null допустим — cellString() его переживёт
        }
        return rows;
    }

    /**
     * Строка-знаменатель: col[0] отсутствует или пустой/нулевой,
     * И хотя бы одна из значимых колонок непуста.
     * Учитываем случай "только предмет без преподавателя" (Химия л/р, п/гр.2 и т.д.)
     */
    private boolean isContinuation(Row row) {
        if (row == null) return false;
        String c0 = cellString(row, COL_NUMBER);
        // col[0] должна быть пустой или нулевой — не номером пары и не днём/группой
        boolean noNumber = c0.isBlank() || isPairZero(c0) || c0.equals("0.0");
        if (!noNumber) return false;
        // Хотя бы одна значимая колонка непуста
        return !cellString(row, COL_SUBJECT).isBlank()
                || !cellString(row, COL_TEACHER).isBlank()
                || !cellString(row, COL_ROOM).isBlank();
    }

    /**
     * Проверяем, является ли строка нулём-заглушкой ("0" или "0.0"),
     * который Excel иногда пишет в merged-cell вместо пустоты.
     * Отличаем от реального номера пары 0 по контексту в isContinuation.
     */
    private boolean isPairZero(String v) {
        return "0".equals(v) || "0.0".equals(v);
    }

    private CellData readCellData(Row row) {
        return new CellData(
                normalizeCell(cellString(row, COL_SUBJECT)),
                normalizeCell(cellString(row, COL_TEACHER)),
                normalizeCell(cellString(row, COL_ROOM))
        );
    }

    /**
     * Excel хранит переносы строк как \n или \r\n.
     * Несколько значений (подгруппы) нормализуем через " / ".
     */
    private String normalizeCell(String value) {
        if (value.isBlank()) return "";
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\n', '/')
                .replaceAll("\\s*/\\s*", " / ")
                .trim();
    }

    private boolean isGroup(String v) {
        return !v.isBlank() && GROUP_PATTERN.matcher(v).find();
    }

    private boolean isDay(String v) {
        return DAYS.contains(v.trim());
    }

    private boolean isPairNumber(String v) {
        return !v.isBlank() && PAIR_NUMBER_PATTERN.matcher(v).matches();
    }

    /**
     * Извлекаем первую группу из строки вида:
     * "ИС1-21/ ИС1-22", "ТТ1-11\ТТ1-12", "ЮР 1-11 / ЮР1-12", "АР1-31"
     */
    private String extractGroup(String v) {
        // Делим по любому сочетанию пробелов + разделителей / \
        String[] parts = v.split("[/\\\\]");
        return parts[0].trim();
    }

    private int parsePairNumber(String v) {
        try {
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse pair number: '{}'", v);
            return -1;
        }
    }

    /**
     * Читаем значение ячейки как строку, независимо от типа (NUMERIC, STRING, FORMULA).
     * toString() у POI для числовых ячеек возвращает "1.0", "2.0" — мы это принимаем.
     */
    private String cellString(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        // Для числовых ячеек toString() даёт "1.0" — нас это устраивает,
        // PAIR_NUMBER_PATTERN это съедает
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            long l = (long) d;
            // Возвращаем "1" вместо "1.0" для целых — чище для логов
            return d == l ? String.valueOf(l) : String.valueOf(d);
        }
        return cell.toString().trim();
    }
}