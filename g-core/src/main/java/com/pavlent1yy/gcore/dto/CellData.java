package com.pavlent1yy.gcore.dto;

import java.util.List;

public record CellData(
        List<String> subjects,
        List<String> teachers,
        List<String> rooms
) {
    public boolean isEmpty() {
        return subjects.isEmpty()
                && teachers.isEmpty()
                && rooms.isEmpty();
    }
}