package com.pavlent1yy.gradinator.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SheetBuilder {

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private Sheet sheet = workbook.createSheet();
    private int rowIndex = 0;

    public SheetBuilder newSheet() {
        sheet = workbook.createSheet();
        rowIndex = 0;
        return this;
    }

    public SheetBuilder group(String name) {
        sheet.createRow(rowIndex++).createCell(0).setCellValue(name);
        return this;
    }

    public SheetBuilder day(String day) {
        sheet.createRow(rowIndex++).createCell(0).setCellValue(day);
        return this;
    }

    public SheetBuilder pair(int number, String subject, String teacher, String room) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(number);
        fill(row, subject, teacher, room);
        return this;
    }

    public SheetBuilder continuation(String subject, String teacher, String room) {
        fill(sheet.createRow(rowIndex++), subject, teacher, room);
        return this;
    }

    public SheetBuilder text(String c0) {
        sheet.createRow(rowIndex++).createCell(0).setCellValue(c0);
        return this;
    }

    public SheetBuilder skipRow() {
        rowIndex++;
        return this;
    }

    public SheetBuilder fullWeek(String group, String subjectPrefix) {
        group(group);
        for (String d : new String[]{"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"}) {
            day(d);
            pair(1, subjectPrefix + " " + d, "Иванов", "101");
            pair(2, subjectPrefix + " 2 " + d, "Петров", "202");
        }
        return this;
    }

    public Sheet sheet() {
        return sheet;
    }

    public Path writeTo(Path file) throws IOException {
        try (OutputStream os = Files.newOutputStream(file)) {
            workbook.write(os);
        }
        workbook.close();
        return file;
    }

    private void fill(Row row, String subject, String teacher, String room) {
        if (subject != null) row.createCell(1).setCellValue(subject);
        if (teacher != null) row.createCell(5).setCellValue(teacher);
        if (room != null) row.createCell(8).setCellValue(room);
    }
}
