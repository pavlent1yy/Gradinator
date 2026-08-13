package com.pavlent1yy.gradinator.controller;

import com.pavlent1yy.gradinator.service.WebParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class WebParserController {
    private final WebParserService parserService;

    @GetMapping("/change-date")
    public LocalDate getActualChangeDate() {
        return parserService.getDateFromChangesURL();
    }
}
