package com.pavlent1yy.gcore.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbsenceType {
    MISSED(2),
    LATE(1);

    private final int hours;
}
