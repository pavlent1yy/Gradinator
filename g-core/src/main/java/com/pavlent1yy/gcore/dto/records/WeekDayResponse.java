package com.pavlent1yy.gcore.dto.records;

import java.time.LocalDate;

public record WeekDayResponse(
        LocalDate date,
        ScheduleResponse schedule
) {}
