package com.pavlent1yy.gradinator.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleModelTest {

    private static CellData empty() {
        return new CellData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    @Test
    void cellDataShortConstructorPutsValuesInRightLists() {
        CellData cell = new CellData("Математика", "101", "Иванов");

        assertThat(cell.getSubjects()).containsExactly("Математика");
        assertThat(cell.getRooms()).containsExactly("101");
        assertThat(cell.getTeachers()).containsExactly("Иванов");
        assertThat(cell.isEmpty()).isFalse();
    }

    @Test
    void cellDataIsEmptyOnlyWhenAllListsEmpty() {
        assertThat(empty().isEmpty()).isTrue();
        assertThat(new CellData(List.of(), List.of("Иванов"), List.of()).isEmpty()).isFalse();
    }

    @Test
    void addPairAppendsNewNumbers() {
        DaySchedule day = new DaySchedule("Понедельник");
        day.addPair(new PairSlot(1, new CellData("А", "1", "Т"), null));
        day.addPair(new PairSlot(2, new CellData("Б", "2", "Т"), null));

        assertThat(day.getPairs()).extracting(PairSlot::getPairNumber).containsExactly(1, 2);
    }

    @Test
    void addPairFillsEmptyNumeratorOfExistingPair() {
        DaySchedule day = new DaySchedule("Понедельник");
        day.addPair(new PairSlot(1, empty(), null));
        day.addPair(new PairSlot(1, new CellData("Математика", "101", "Иванов"), null));

        assertThat(day.getPairs()).hasSize(1);
        assertThat(day.getPairs().get(0).getNumerator().getSubjects()).containsExactly("Математика");
    }

    @Test
    void addPairDoesNotOverwriteFilledNumerator() {
        DaySchedule day = new DaySchedule("Понедельник");
        day.addPair(new PairSlot(1, new CellData("Математика", "101", "Иванов"), null));
        day.addPair(new PairSlot(1, new CellData("Физика", "202", "Петров"), null));

        assertThat(day.getPairs().get(0).getNumerator().getSubjects()).containsExactly("Математика");
    }

    @Test
    void addPairFillsMissingDenominatorOnly() {
        DaySchedule day = new DaySchedule("Понедельник");
        CellData den = new CellData("Физика", "202", "Петров");
        day.addPair(new PairSlot(1, new CellData("Математика", "101", "Иванов"), null));
        day.addPair(new PairSlot(1, empty(), den));
        day.addPair(new PairSlot(1, empty(), new CellData("Химия", "303", "Сидоров")));

        assertThat(day.getPairs().get(0).getDenominator()).isSameAs(den);
    }

    @Test
    void dayToStringShowsBothWeeks() {
        DaySchedule day = new DaySchedule("Среда");
        day.addPair(new PairSlot(1, new CellData("Математика", "101", "Иванов"),
                new CellData("Физика", "202", "Петров")));

        assertThat(day.toString())
                .contains("=== Среда ===")
                .contains("[1]")
                .contains("Ч: ")
                .contains(" | З: ");
    }

    @Test
    void groupScheduleIgnoresDuplicatedDay() {
        GroupSchedule group = new GroupSchedule("ИС1-33");
        DaySchedule first = new DaySchedule("Понедельник");
        group.addDay(first);
        group.addDay(new DaySchedule("Понедельник"));
        group.addDay(new DaySchedule("Вторник"));

        assertThat(group.getDays()).hasSize(2);
        assertThat(group.getDays().get(0)).isSameAs(first);
    }
}
