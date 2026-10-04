package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WeekServiceTest {

    private final WeekService weekService = new WeekService();

    @ParameterizedTest
    @CsvSource({
            "2026-09-01, NUMERATOR",
            "2026-09-06, NUMERATOR",
            "2026-09-07, DENOMINATOR",
            "2026-09-13, DENOMINATOR",
            "2026-09-14, NUMERATOR",
            "2026-10-05, DENOMINATOR",
            "2026-10-12, NUMERATOR"
    })
    void weekTypeAlternatesFromFirstAcademicMonday(LocalDate date, WeekType expected) {
        assertThat(weekService.getWeekTypeByDate(date)).isEqualTo(expected);
    }

    @Test
    void springSemesterCountsFromPreviousSeptember() {
        LocalDate start = LocalDate.of(2026, 8, 31);
        LocalDate march = LocalDate.of(2027, 3, 1);
        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(start, march);

        WeekType expected = weeks % 2 == 0 ? WeekType.NUMERATOR : WeekType.DENOMINATOR;
        assertThat(weekService.getWeekTypeByDate(march)).isEqualTo(expected);
    }

    @Test
    void wholeWeekHasSameType() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        WeekType type = weekService.getWeekTypeByDate(monday);

        for (int i = 1; i < 7; i++) {
            assertThat(weekService.getWeekTypeByDate(monday.plusDays(i))).isEqualTo(type);
        }
    }

    @Test
    void currentWeekTypeMatchesToday() {
        assertThat(weekService.getCurrentWeekType())
                .isEqualTo(weekService.getWeekTypeByDate(LocalDate.now()));
    }

    @Test
    void onlySundayIsDayOff() {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        for (int i = 0; i < 6; i++) {
            assertThat(weekService.isDayOff(monday.plusDays(i))).isFalse();
        }
        assertThat(weekService.isDayOff(monday.plusDays(6))).isTrue();
    }
}
