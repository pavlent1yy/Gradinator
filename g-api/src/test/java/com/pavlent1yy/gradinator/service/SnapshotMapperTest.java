package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotMapperTest {

    private final SnapshotMapper mapper = new SnapshotMapper();

    private static DaySchedule day(PairSlot... pairs) {
        DaySchedule day = new DaySchedule("Понедельник");
        day.setPairs(new ArrayList<>(List.of(pairs)));
        return day;
    }

    private static PairSlot pair(int number, String subject) {
        return new PairSlot(number, new CellData(subject, "101", "Иванов"), null);
    }

    @Test
    void hashIsSha256Hex() {
        String hash = mapper.computeHash(Map.of("ИС1-33", day(pair(1, "Математика"))));

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hashDoesNotDependOnGroupOrPairOrder() {
        Map<String, DaySchedule> first = new LinkedHashMap<>();
        first.put("ИС1-33", day(pair(1, "Математика"), pair(2, "Физика")));
        first.put("СА1-21", day(pair(1, "История")));

        Map<String, DaySchedule> second = new LinkedHashMap<>();
        second.put("СА1-21", day(pair(1, "История")));
        second.put("ИС1-33", day(pair(2, "Физика"), pair(1, "Математика")));

        assertThat(mapper.computeHash(first)).isEqualTo(mapper.computeHash(second));
    }

    @Test
    void hashChangesWhenContentChanges() {
        String before = mapper.computeHash(Map.of("ИС1-33", day(pair(1, "Математика"))));
        String after = mapper.computeHash(Map.of("ИС1-33", day(pair(1, "Физика"))));

        assertThat(before).isNotEqualTo(after);
    }

    @Test
    void hashSeesDenominator() {
        PairSlot withDenominator = pair(1, "Математика");
        withDenominator.setDenominator(new CellData("Физика", "202", "Петров"));

        String plain = mapper.computeHash(Map.of("ИС1-33", day(pair(1, "Математика"))));
        String withDen = mapper.computeHash(Map.of("ИС1-33", day(withDenominator)));

        assertThat(plain).isNotEqualTo(withDen);
    }

    @Test
    void hashOfEmptyMapIsStable() {
        assertThat(mapper.computeHash(Map.of())).isEqualTo(mapper.computeHash(new HashMap<>()));
    }

    @Test
    void toEntriesMapsEveryPair() {
        ScheduleSnapshot snapshot = ScheduleSnapshot.builder().id(7L).build();
        PairSlot second = pair(2, "Физика");
        second.setDenominator(new CellData("Химия", "303", "Сидоров"));

        List<ScheduleEntry> entries = mapper.toEntries(
                snapshot,
                Map.of("ИС1-33", day(pair(1, "Математика"), second)),
                Map.of("ИС1-33", Set.of(2))
        );

        assertThat(entries).hasSize(2);

        ScheduleEntry first = entries.get(0);
        assertThat(first.getSnapshot()).isSameAs(snapshot);
        assertThat(first.getGroupName()).isEqualTo("ИС1-33");
        assertThat(first.getDay()).isEqualTo("Понедельник");
        assertThat(first.getPairNumber()).isEqualTo(1);
        assertThat(first.isHasChanges()).isFalse();
        assertThat(first.getNumeratorSubjects()).containsExactly("Математика");
        assertThat(first.getNumeratorTeachers()).containsExactly("Иванов");
        assertThat(first.getNumeratorRooms()).containsExactly("101");
        assertThat(first.getDenominatorSubjects()).isEmpty();
        assertThat(first.getDenominatorTeachers()).isEmpty();
        assertThat(first.getDenominatorRooms()).isEmpty();

        ScheduleEntry changed = entries.get(1);
        assertThat(changed.isHasChanges()).isTrue();
        assertThat(changed.getDenominatorSubjects()).containsExactly("Химия");
    }

    @Test
    void toEntriesHandlesNullNumeratorAndMissingChanges() {
        List<ScheduleEntry> entries = mapper.toEntries(
                new ScheduleSnapshot(),
                Map.of("ИС1-33", day(new PairSlot(3, null, null))),
                Map.of()
        );

        assertThat(entries).singleElement().satisfies(e -> {
            assertThat(e.getNumeratorSubjects()).isEmpty();
            assertThat(e.getDenominatorSubjects()).isEmpty();
            assertThat(e.isHasChanges()).isFalse();
        });
    }
}
