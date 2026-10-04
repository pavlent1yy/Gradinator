package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.config.StorageContext;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.service.parser.ExcelLayoutScanner;
import com.pavlent1yy.gradinator.support.SheetBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);

    @TempDir
    Path storageDir;

    private final GroupFileMap groupFileMap = mock(GroupFileMap.class);
    private ScheduleService service;

    @BeforeEach
    void setUp() throws Exception {
        StorageContext storage = new StorageContext();
        ReflectionTestUtils.setField(storage, "storageDirPath", storageDir.toString());
        storage.init();

        new SheetBuilder()
                .fullWeek("ИС1-33", "Предмет")
                .newSheet()
                .fullWeek("СА1-21", "Другой")
                .group("ЮР1-11")
                .day("Понедельник")
                .writeTo(storageDir.resolve("oit.xlsx"));

        when(groupFileMap.getPossibleFileNameByGroup(anyString())).thenReturn("oit.xlsx");
        when(groupFileMap.getPossibleFileNameByGroup("ЮР1-11")).thenReturn(null);
        when(groupFileMap.getAllFiles()).thenReturn(Set.of("oit.xlsx"));

        service = new ScheduleService(new ExcelLayoutScanner(), storage, groupFileMap);
    }

    private static PairSlot change(int number, String subject, String room) {
        PairSlot pair = new PairSlot();
        pair.setPairNumber(number);
        pair.setNumerator(new CellData(subject, room, "в предмете"));
        return pair;
    }

    @Test
    void readsGroupsFromAllSheets() {
        List<GroupSchedule> all = service.getGroupSchedule("ИС1-33");

        assertThat(all).extracting(GroupSchedule::getGroup).containsExactly("ИС1-33", "СА1-21", "ЮР1-11");
    }

    @Test
    void missingFileIsReportedAsFileNotFound() {
        when(groupFileMap.getPossibleFileNameByGroup("ИС1-33")).thenReturn("missing.xlsx");

        assertThatThrownBy(() -> service.getGroupSchedule("ИС1-33"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    void getWeekReturnsRequestedGroup() {
        GroupSchedule week = service.getWeek("СА1-21");

        assertThat(week.getGroup()).isEqualTo("СА1-21");
        assertThat(week.getDays()).hasSize(6);
    }

    @Test
    void getWeekFailsForUnknownGroup() {
        assertThatThrownBy(() -> service.getWeek("ИБ1-11"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("ИБ1-11");
    }

    @Test
    void scheduleForDateTakesDayByWeekday() {
        DaySchedule wednesday = service.getScheduleForDate("ИС1-33", WEDNESDAY, List.of());

        assertThat(wednesday.getDay()).isEqualTo("Среда");
        assertThat(wednesday.getPairs()).extracting(PairSlot::getPairNumber).containsExactly(1, 2);
        assertThat(wednesday.getPairs().get(0).getNumerator().getSubjects()).containsExactly("Предмет Среда");
    }

    @Test
    void changesReplaceAndExtendPairsSorted() {
        DaySchedule monday = service.getScheduleForDate("ИС1-33", MONDAY, List.of(
                change(4, "❗ Химия", "404"),
                change(2, "❗ Физика", "303")
        ));

        assertThat(monday.getPairs()).extracting(PairSlot::getPairNumber).containsExactly(1, 2, 4);
        assertThat(monday.getPairs().get(1).getNumerator().getSubjects()).containsExactly("❗ Физика");
        assertThat(monday.getPairs().get(2).getNumerator().getRooms()).containsExactly("404");
    }

    @Test
    void accordingToScheduleChangeKeepsOriginalSubjectAndTeacher() {
        DaySchedule monday = service.getScheduleForDate("ИС1-33", MONDAY, List.of(
                change(1, "❗ По расписанию", "505")
        ));

        CellData numerator = monday.getPairs().get(0).getNumerator();
        assertThat(numerator.getSubjects()).containsExactly("Предмет Понедельник");
        assertThat(numerator.getTeachers()).containsExactly("Иванов");
        assertThat(numerator.getRooms()).containsExactly("505");
    }

    @Test
    void accordingToScheduleForUnknownPairStaysAsIs() {
        DaySchedule monday = service.getScheduleForDate("ИС1-33", MONDAY, List.of(
                change(6, "по расписанию", "505")
        ));

        assertThat(monday.getPairs().get(2).getNumerator().getSubjects()).containsExactly("по расписанию");
    }

    @Test
    void sundayIsRejected() {
        assertThatThrownBy(() -> service.getScheduleForDate("ИС1-33", MONDAY.minusDays(1), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collectsOnlyKnownGroupsFromFiles() {
        assertThat(service.getAllGroupsFromFiles()).containsExactlyInAnyOrder("ИС1-33", "СА1-21");
    }

    @Test
    void checkGroupSyncDoesNotFail() {
        service.checkGroupSync(Set.of("ИС1-33", "СА1-21"), Set.of("ИС1-33"));
        service.checkGroupSync(Set.of(), Set.of());
    }
}
