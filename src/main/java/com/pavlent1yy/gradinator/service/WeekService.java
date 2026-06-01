package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

import static com.pavlent1yy.gradinator.enums.WeekType.DENOMINATOR;
import static com.pavlent1yy.gradinator.enums.WeekType.NUMERATOR;


@Service
public class WeekService {
    public WeekType getWeekType() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        LocalDate startOfAcademicYear = LocalDate.of(currentYear, Month.SEPTEMBER, 1);

        if (today.isBefore(startOfAcademicYear)) {
            startOfAcademicYear = LocalDate.of(currentYear - 1, Month.SEPTEMBER, 1);
        }

        long weeksBetween = ChronoUnit.WEEKS.between(startOfAcademicYear, today);
        return weeksBetween % 2 == 0 ? DENOMINATOR : NUMERATOR;
    }

}
