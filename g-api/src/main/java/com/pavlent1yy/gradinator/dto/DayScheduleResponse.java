package com.pavlent1yy.gradinator.dto;

import java.time.LocalDate;
import java.util.List;

public record DayScheduleResponse(String group, String day, LocalDate date, List<PairResponse> pairs) {}
