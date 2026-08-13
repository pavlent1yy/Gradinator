package com.pavlent1yy.gcore.dto;

import java.time.LocalDate;
import java.util.List;

public record ScheduleResponse(
        String group,
        String day,
        LocalDate date,
        String weekType,
        List<PairResponse> pairs
) {}