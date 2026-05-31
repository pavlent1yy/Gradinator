package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.parser.ExcelLayoutScanner;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@AllArgsConstructor
public class TestController {

    private final ExcelLayoutScanner scanner;

    @PostMapping("/parse/{fileName}")
    public List<GroupSchedule> parse(@PathVariable String fileName) throws Exception {
        try (FileInputStream fis = new FileInputStream(
                String.format("C:/Users/User/Documents/scheduleFiles/%s", fileName));
             Workbook wb = new XSSFWorkbook(fis)) {

            List<GroupSchedule> result = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                result.addAll(scanner.scan(wb.getSheetAt(i)));
            }
            return result;
        }
    }
}
