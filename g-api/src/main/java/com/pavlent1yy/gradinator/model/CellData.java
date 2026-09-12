package com.pavlent1yy.gradinator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CellData {
    private List<String> subjects;
    private List<String> teachers;
    private List<String> rooms;

    public CellData (String subject, String room, String teacher){
        subjects = new ArrayList<>(List.of(subject));
        rooms = new ArrayList<>(List.of(room));
        teachers = new ArrayList<>(List.of(teacher));
    }

    @JsonProperty("empty")
    public boolean isEmpty() {
        return subjects.isEmpty() && teachers.isEmpty() && rooms.isEmpty();
    }
    
}
