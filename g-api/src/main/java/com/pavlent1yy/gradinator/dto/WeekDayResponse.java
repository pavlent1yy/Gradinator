package com.pavlent1yy.gradinator.dto;

import java.time.LocalDate;

public record WeekDayResponse(
        LocalDate date,
        DayScheduleResponse schedule
) {}
