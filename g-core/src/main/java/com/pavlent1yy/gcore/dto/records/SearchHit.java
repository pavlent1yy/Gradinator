package com.pavlent1yy.gcore.dto.records;

import java.util.List;

public record SearchHit(
        String group,
        int pairNumber,
        List<String> subjects,
        List<String> teachers,
        List<String> rooms,
        boolean hasChanges
) {}
