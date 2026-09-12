package com.pavlent1yy.gcore.dto.records;

import com.pavlent1yy.gcore.enums.WeekType;

import java.time.LocalDate;
import java.util.List;

public record ScheduleResponse(
        String group,
        String day,
        WeekType weekType,
        LocalDate date,
        List<PairResponse> pairs
) {}