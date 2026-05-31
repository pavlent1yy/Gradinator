package com.pavlent1yy.gradinator.pojo;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

class GroupSchedule {
    String group;
    Map<DayOfWeek, List<LessonSlot>> days;
}
