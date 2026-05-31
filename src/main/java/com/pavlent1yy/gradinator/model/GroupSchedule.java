package com.pavlent1yy.gradinator.model;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class GroupSchedule {
    private String group;
    private List<DaySchedule> days = new ArrayList<>();

    public GroupSchedule(String group) {
        this.group = group;
    }

    public void addDay(DaySchedule day) {
        this.days.add(day);
    }
}
