package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.GroupSchedule;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.support.SheetBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelLayoutScannerTest {

    private final ExcelLayoutScanner scanner = new ExcelLayoutScanner();

    @Test
    void parsesGroupDaysAndPairs() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Понедельник")
                .pair(1, "Математика", "Иванов", "101")
                .pair(2, "Физика", "Петров", "202")
                .day("Вторник")
                .pair(3, "История", "Сидоров", "303")
                .sheet();

        List<GroupSchedule> result = scanner.scan(sheet);

        assertThat(result).hasSize(1);
        GroupSchedule group = result.get(0);
        assertThat(group.getGroup()).isEqualTo("ИС1-33");
        assertThat(group.getDays()).extracting(DaySchedule::getDay)
                .containsExactly("Понедельник", "Вторник");

        PairSlot first = group.getDays().get(0).getPairs().get(0);
        assertThat(first.getPairNumber()).isEqualTo(1);
        assertThat(first.getNumerator().getSubjects()).containsExactly("Математика");
        assertThat(first.getNumerator().getTeachers()).containsExactly("Иванов");
        assertThat(first.getNumerator().getRooms()).containsExactly("101");
        assertThat(first.getDenominator()).isNull();
    }

    @Test
    void continuationRowBecomesDenominator() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Среда")
                .pair(1, "Математика", "Иванов", "101")
                .continuation("Физика", "Петров", "202")
                .sheet();

        PairSlot pair = scanner.scan(sheet).get(0).getDays().get(0).getPairs().get(0);

        assertThat(pair.getNumerator().getSubjects()).containsExactly("Математика");
        assertThat(pair.getDenominator().getSubjects()).containsExactly("Физика");
        assertThat(pair.getDenominator().getTeachers()).containsExactly("Петров");
        assertThat(pair.getDenominator().getRooms()).containsExactly("202");
    }

    @Test
    void multilineCellsAreSplitAndTrimmed() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Четверг")
                .pair(1, "Информатика 1 п/г\r\n Информатика 2 п/г \n", "Иванов\nПетров", "101\r202")
                .sheet();

        PairSlot pair = scanner.scan(sheet).get(0).getDays().get(0).getPairs().get(0);

        assertThat(pair.getNumerator().getSubjects()).containsExactly("Информатика 1 п/г", "Информатика 2 п/г");
        assertThat(pair.getNumerator().getTeachers()).containsExactly("Иванов", "Петров");
        assertThat(pair.getNumerator().getRooms()).containsExactly("101", "202");
    }

    @Test
    void emptyPairRowGivesEmptyNumerator() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Пятница")
                .pair(4, null, null, null)
                .sheet();

        PairSlot pair = scanner.scan(sheet).get(0).getDays().get(0).getPairs().get(0);

        assertThat(pair.getPairNumber()).isEqualTo(4);
        assertThat(pair.getNumerator().isEmpty()).isTrue();
    }

    @Test
    void recognizesSeveralGroupsOnOneSheet() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Понедельник")
                .pair(1, "Математика", "Иванов", "101")
                .group("СА1-21")
                .day("Понедельник")
                .pair(1, "Физика", "Петров", "202")
                .sheet();

        List<GroupSchedule> result = scanner.scan(sheet);

        assertThat(result).extracting(GroupSchedule::getGroup).containsExactly("ИС1-33", "СА1-21");
        assertThat(result.get(1).getDays().get(0).getPairs().get(0).getNumerator().getSubjects())
                .containsExactly("Физика");
    }

    @Test
    void groupNameIsNormalized() {
        var sheet = new SheetBuilder()
                .group("ИС 1-33 / курс 3")
                .group("МРТ1-21\\ доп")
                .group("21 МРТ")
                .sheet();

        assertThat(scanner.scan(sheet)).extracting(GroupSchedule::getGroup)
                .containsExactly("ИС1-33", "МРТ1-21", "21 МРТ");
    }

    @Test
    void dayNameWithNonBreakingSpacesIsRecognized() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day(" Суббота​")
                .pair(1, "Физкультура", "Иванов", "спортзал")
                .sheet();

        GroupSchedule group = scanner.scan(sheet).get(0);

        assertThat(group.getDays()).extracting(DaySchedule::getDay).containsExactly("Суббота");
    }

    @Test
    void footerUnknownAndEmptyRowsAreIgnored() {
        var sheet = new SheetBuilder()
                .text("Расписание на семестр")
                .group("ИС1-33")
                .day("Понедельник")
                .skipRow()
                .pair(1, "Математика", "Иванов", "101")
                .text("* примечание")
                .text("какой-то текст")
                .sheet();

        GroupSchedule group = scanner.scan(sheet).get(0);

        assertThat(group.getDays()).hasSize(1);
        assertThat(group.getDays().get(0).getPairs()).hasSize(1);
    }

    @Test
    void rowsWithoutContextAreSkipped() {
        var sheet = new SheetBuilder()
                .day("Понедельник")
                .pair(1, "Математика", "Иванов", "101")
                .continuation("Сирота", "Никто", "000")
                .sheet();

        assertThat(scanner.scan(sheet)).isEmpty();
    }

    @Test
    void pairBeforeAnyDayInGroupIsSkipped() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .pair(1, "Математика", "Иванов", "101")
                .day("Понедельник")
                .sheet();

        GroupSchedule group = scanner.scan(sheet).get(0);

        assertThat(group.getDays()).hasSize(1);
        assertThat(group.getDays().get(0).getPairs()).isEmpty();
    }

    @Test
    void duplicatedPairNumberIsMergedIntoFirst() {
        var sheet = new SheetBuilder()
                .group("ИС1-33")
                .day("Понедельник")
                .pair(1, null, null, null)
                .pair(1, "Математика", "Иванов", "101")
                .sheet();

        List<PairSlot> pairs = scanner.scan(sheet).get(0).getDays().get(0).getPairs();

        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0).getNumerator().getSubjects()).containsExactly("Математика");
    }

    @Test
    void emptySheetGivesNoGroups() {
        assertThat(scanner.scan(new SheetBuilder().sheet())).isEmpty();
    }
}
