package com.pavlent1yy.gcore.dto.records;

import java.time.LocalDate;

public record AbsenceStatsResponse(
        PeriodStats week,
        PeriodStats month,
        PeriodStats semester,
        PeriodStats total
) {
    public record PeriodStats(
            LocalDate from,
            LocalDate to,
            int hours,
            int missedPairs,
            int lates
    ) {}
}
