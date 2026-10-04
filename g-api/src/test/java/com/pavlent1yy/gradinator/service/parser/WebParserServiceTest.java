package com.pavlent1yy.gradinator.service.parser;

import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.model.PairSlot;
import com.pavlent1yy.gradinator.service.WeekService;
import com.pavlent1yy.gradinator.support.StubHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebParserServiceTest {

    private final WeekService weekService = mock(WeekService.class);
    private StubHttpServer server;
    private WebParserService service;

    @BeforeEach
    void setUp() throws Exception {
        server = new StubHttpServer();
        when(weekService.getCurrentWeekType()).thenReturn(WeekType.NUMERATOR);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private void init(String firstPath, String secondPath) {
        service = new WebParserService(weekService);
        ReflectionTestUtils.setField(service, "firstShiftUrl", server.url(firstPath));
        ReflectionTestUtils.setField(service, "secondShiftUrl", server.url(secondPath));
        ReflectionTestUtils.invokeMethod(service, "uploadTimetableURLs");
    }

    private static String page(String date, String... rows) {
        StringBuilder sb = new StringBuilder("<html><body><p>Изменения в расписании на ")
                .append(date)
                .append(" г.</p><div><table><tbody>")
                .append("<tr><td>№</td><td>Группа</td><td>Пара</td><td>Было</td><td>Стало</td><td>Ауд.</td></tr>");
        for (String row : rows) sb.append(row);
        return sb.append("</tbody></table></div></body></html>").toString();
    }

    private static String row(String group, String pair, String subject, String room) {
        return "<tr><td>1</td><td>" + group + "</td><td>" + pair + "</td><td>старое</td><td>"
                + subject + "</td><td>" + room + "</td></tr>";
    }

    @Test
    void parsesChangesFromBothShifts() {
        server.html("/first", page("5 октября 2026", row("ИС1-33", "2", "Химия", "404")))
                .html("/second", page("5 октября 2026", row("СА1-21", "1", "История", "101")));
        init("/first", "/second");

        WebParserService.AllChanges changes = service.getAllChanges();

        assertThat(changes.date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(changes.byGroup()).containsOnlyKeys("ИС1-33", "СА1-21");

        PairSlot pair = changes.forGroup("ИС1-33").get(0);
        assertThat(pair.getPairNumber()).isEqualTo(2);
        assertThat(pair.getNumerator().getSubjects()).containsExactly("❗ Химия");
        assertThat(pair.getNumerator().getRooms()).containsExactly("404");
        assertThat(pair.getNumerator().getTeachers()).containsExactly("в предмете");
        assertThat(pair.getDenominator()).isNull();
    }

    @Test
    void expandsListsAndRangesOfPairs() {
        server.html("/first", page("5 октября 2026",
                        row("ИС1-33", "1,3", "Химия", "404"),
                        row("СА1-21", "2-4", "Физика", "202")))
                .html("/second", page("5 октября 2026"));
        init("/first", "/second");

        WebParserService.AllChanges changes = service.getAllChanges();

        assertThat(changes.forGroup("ИС1-33")).extracting(PairSlot::getPairNumber).containsExactly(1, 3);
        assertThat(changes.forGroup("СА1-21")).extracting(PairSlot::getPairNumber).containsExactly(2, 3, 4);
    }

    @Test
    void cancelledPairIsMarked() {
        server.html("/first", page("5 октября 2026", row("ИС1-33", "2", " СНЯТО ", "")))
                .html("/second", page("5 октября 2026"));
        init("/first", "/second");

        PairSlot pair = service.getAllChanges().forGroup("ИС1-33").get(0);

        assertThat(pair.getNumerator().getSubjects()).containsExactly("❕ Снято");
    }

    @Test
    void denominatorWeekFillsDenominator() {
        when(weekService.getCurrentWeekType()).thenReturn(WeekType.DENOMINATOR);
        server.html("/first", page("5 октября 2026", row("ИС1-33", "2", "Химия", "404")))
                .html("/second", page("5 октября 2026"));
        init("/first", "/second");

        PairSlot pair = service.getAllChanges().forGroup("ИС1-33").get(0);

        assertThat(pair.getNumerator()).isNull();
        assertThat(pair.getDenominator().getSubjects()).containsExactly("❗ Химия");
    }

    @Test
    void skipsHeaderShortAndEmptyGroupRows() {
        server.html("/first", page("5 октября 2026",
                        "<tr><td>a</td><td>b</td></tr>",
                        row(" ", "1", "Химия", "404"),
                        row("ИС1-33", "Пара", "Химия", "404")))
                .html("/second", page("5 октября 2026"));
        init("/first", "/second");

        assertThat(service.getAllChanges().byGroup()).isEmpty();
    }

    @Test
    void brokenShiftDoesNotLoseTheOther() {
        server.status("/first", 500)
                .html("/second", page("6 октября 2026", row("СА1-21", "1", "История", "101")));
        init("/first", "/second");

        WebParserService.AllChanges changes = service.getAllChanges();

        assertThat(changes.date()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(changes.forGroup("СА1-21")).hasSize(1);
        assertThat(changes.forGroup("ИС1-33")).isEmpty();
    }

    @Test
    void pageWithoutDateIsSkipped() {
        server.html("/first", "<html><body><div><table><tbody>"
                        + row("ИС1-33", "2", "Химия", "404") + "</tbody></table></div></body></html>")
                .html("/second", page("5 октября 2026"));
        init("/first", "/second");

        WebParserService.AllChanges changes = service.getAllChanges();

        assertThat(changes.date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(changes.byGroup()).isEmpty();
    }

    @Test
    void bothShiftsDownGiveEmptyResult() {
        server.status("/first", 500).status("/second", 500);
        init("/first", "/second");

        WebParserService.AllChanges changes = service.getAllChanges();

        assertThat(changes.date()).isNull();
        assertThat(changes.byGroup()).isEmpty();
    }

    @Test
    void changeDateUsesFirstShift() {
        server.html("/first", page("5 октября 2026")).html("/second", page("5 октября 2026"));
        init("/first", "/second");

        assertThat(service.getDateFromChangesURL()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void changeDatePrefersFirstShiftOnMismatch() {
        server.html("/first", page("5 октября 2026")).html("/second", page("6 октября 2026"));
        init("/first", "/second");

        assertThat(service.getDateFromChangesURL()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void changeDateFallsBackToAvailableShift() {
        server.status("/first", 500).html("/second", page("6 октября 2026"))
                .html("/third", page("7 октября 2026")).status("/fourth", 500);

        init("/first", "/second");
        assertThat(service.getDateFromChangesURL()).isEqualTo(LocalDate.of(2026, 10, 6));

        init("/third", "/fourth");
        assertThat(service.getDateFromChangesURL()).isEqualTo(LocalDate.of(2026, 10, 7));
    }

    @Test
    void changeDateFailsWhenBothShiftsDown() {
        server.status("/first", 500).status("/second", 500);
        init("/first", "/second");

        assertThatThrownBy(() -> service.getDateFromChangesURL())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forGroupReturnsEmptyListForUnknownGroup() {
        var changes = new WebParserService.AllChanges(null, java.util.Map.of());

        assertThat(changes.forGroup("ИС1-33")).isEqualTo(List.of());
    }
}
