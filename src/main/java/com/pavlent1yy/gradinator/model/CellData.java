package com.pavlent1yy.gradinator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CellData {
    private List<String> subjects;
    private List<String> teachers;
    private List<String> rooms;

    @JsonProperty("empty")
    public boolean isEmpty() {
        return subjects.isEmpty() && teachers.isEmpty() && rooms.isEmpty();
    }
}
