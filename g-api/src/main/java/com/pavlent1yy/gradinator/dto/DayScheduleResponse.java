package com.pavlent1yy.gradinator.dto;

import com.pavlent1yy.gradinator.enums.WeekType;

import java.time.LocalDate;
import java.util.List;

public record DayScheduleResponse(String group, String day, WeekType weekType, LocalDate date, List<PairResponse> pairs) {}
