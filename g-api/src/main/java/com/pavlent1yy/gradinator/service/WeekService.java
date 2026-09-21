package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import static com.pavlent1yy.gradinator.enums.WeekType.DENOMINATOR;
import static com.pavlent1yy.gradinator.enums.WeekType.NUMERATOR;


@Service
public class WeekService {
    public WeekType getCurrentWeekType() {
        LocalDate today = LocalDate.now();
        return getWeekTypeByDate(today);
    }

    public WeekType getWeekTypeByDate(LocalDate date) {
        int academicYear = date.getMonthValue() >= Month.SEPTEMBER.getValue()
                ? date.getYear()
                : date.getYear() - 1;

        LocalDate septemberFirst = LocalDate.of(academicYear, Month.SEPTEMBER, 1);

        LocalDate firstAcademicWeekStart = septemberFirst.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );

        long weeksBetween = ChronoUnit.WEEKS.between(firstAcademicWeekStart, date);

        return weeksBetween % 2 == 0
                ? DENOMINATOR
                : NUMERATOR;
    }

}
