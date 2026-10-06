package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.client.GApiClient;
import com.pavlent1yy.gcore.dto.records.CellData;
import com.pavlent1yy.gcore.dto.records.FreeRoomsResponse;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.dto.records.SearchHit;
import com.pavlent1yy.gcore.enums.SearchType;
import com.pavlent1yy.gcore.enums.WeekType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 5);

    private GApiClient client;
    private SearchService service;

    private static CellData cell(String subject, String teacher, String room) {
        return new CellData(List.of(subject), List.of(teacher), List.of(room));
    }

    private static CellData empty() {
        return new CellData(List.of(), List.of(), List.of());
    }

    @BeforeEach
    void setUp() {
        client = mock(GApiClient.class);
        service = new SearchService(client);
        when(client.getAllSchedules(DAY)).thenReturn(Map.of(
                "ИС1-33", new ScheduleResponse("ИС1-33", "Понедельник", WeekType.DENOMINATOR, DAY, List.of(
                        new PairResponse(1, cell("Математика", "Шереметьева Н.В.", "М106"), empty(), false),
                        new PairResponse(2, cell("Физика", "Иванов И.И.", "А201"), cell("Химия", "Петрова П.П.", "А202"), true)
                )),
                "СА1-21", new ScheduleResponse("СА1-21", "Понедельник", WeekType.DENOMINATOR, DAY, List.of(
                        new PairResponse(1, cell("История", "Иванов И.И.", "А201,А203"), empty(), false),
                        new PairResponse(2, cell("Черчение", "Сидоров С.С.", "М106"), empty(), false)
                ))
        ));
    }

    @Test
    void findsTeacherAcrossGroupsUsingActiveWeekCell() {
        List<SearchHit> hits = service.search("иванов", SearchType.TEACHER, DAY);

        assertThat(hits).extracting(SearchHit::group, SearchHit::pairNumber)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("СА1-21", 1));
    }

    @Test
    void anySearchMatchesSubjectsAndRoomsCaseInsensitive() {
        assertThat(service.search("ХИМ", SearchType.ANY, DAY)).extracting(SearchHit::group).containsExactly("ИС1-33");
        assertThat(service.search("м106", SearchType.ROOM, DAY)).extracting(SearchHit::pairNumber).containsExactly(1, 2);
    }

    @Test
    void ignoresTooShortQuery() {
        assertThat(service.search(" и ", SearchType.ANY, DAY)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void cachesDaySchedule() {
        service.search("физ", SearchType.ANY, DAY);
        service.search("хим", SearchType.ANY, DAY);
        service.freeRooms(DAY);

        verify(client, times(1)).getAllSchedules(DAY);
    }

    @Test
    void freeRoomsAreDayRoomsMinusBusyPerPair() {
        List<FreeRoomsResponse> rooms = service.freeRooms(DAY);

        assertThat(rooms).extracting(FreeRoomsResponse::pairNumber).containsExactly(1, 2);
        assertThat(rooms.get(0).busyRooms()).containsExactly("А201", "А203", "М106");
        assertThat(rooms.get(0).freeRooms()).containsExactly("А202");
        assertThat(rooms.get(1).freeRooms()).containsExactly("А201", "А203");
    }

    @Test
    void cleansDictionaries() {
        assertThat(SearchService.cleanDictionary(List.of(".", "", "Иванов  И.И.", "иванов и.и.", "А201")))
                .containsExactly("А201", "Иванов И.И.");
    }
}
