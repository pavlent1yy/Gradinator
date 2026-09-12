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
    public WeekType getCurrentWeekType() {
        LocalDate today = LocalDate.now();
        return getWeekTypeByDate(today);
    }

    public WeekType getWeekTypeByDate(LocalDate date){
        int currentYear = date.getYear();
        LocalDate startOfAcademicYear = LocalDate.of(currentYear, Month.SEPTEMBER, 1);

        if (date.isBefore(startOfAcademicYear)) {
            startOfAcademicYear = LocalDate.of(currentYear - 1, Month.SEPTEMBER, 1);
        }

        long weeksBetween = ChronoUnit.WEEKS.between(startOfAcademicYear, date);
        return weeksBetween % 2 == 0 ? DENOMINATOR : NUMERATOR;
    }

}
