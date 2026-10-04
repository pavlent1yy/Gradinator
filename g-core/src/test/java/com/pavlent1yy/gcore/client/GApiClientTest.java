package com.pavlent1yy.gcore.client;

import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.enums.WeekType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GApiClientTest {

    private static final String BASE = "http://g-api.test";

    private MockRestServiceServer server;
    private GApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GApiClient(builder.build());
    }

    @Test
    void getsScheduleForGroupAndDate() {
        server.expect(requestTo(BASE + "/api/schedule?group=%D0%98%D0%A11-33&date=2026-10-05"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"group":"ИС1-33","day":"Понедельник","weekType":"DENOMINATOR","date":"2026-10-05",
                         "pairs":[{"pairNumber":1,
                                   "numerator":{"subjects":["Математика"],"teachers":["Иванов"],"rooms":["101"],"empty":false},
                                   "denominator":null,"hasChanges":true}]}
                        """, MediaType.APPLICATION_JSON));

        ScheduleResponse response = client.getSchedule("ИС1-33", LocalDate.of(2026, 10, 5));

        assertThat(response.group()).isEqualTo("ИС1-33");
        assertThat(response.weekType()).isEqualTo(WeekType.DENOMINATOR);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(response.pairs()).singleElement().satisfies(p -> {
            assertThat(p.pairNumber()).isEqualTo(1);
            assertThat(p.hasChanges()).isTrue();
            assertThat(p.numerator().subjects()).containsExactly("Математика");
            assertThat(p.denominator()).isNull();
        });
        server.verify();
    }

    @Test
    void notFoundIsWrappedScheduleNotFound() {
        server.expect(requestTo(BASE + "/api/schedule?group=X&date=2026-10-05"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getSchedule("X", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(ScheduleNotFoundException.class)
                .hasRootCauseMessage("Нет актуальных данных для группы 'X' на 2026-10-05");
    }

    @Test
    void serverErrorIsPropagated() {
        server.expect(requestTo(BASE + "/api/schedule?group=X&date=2026-10-05")).andRespond(withServerError());

        assertThatThrownBy(() -> client.getSchedule("X", LocalDate.of(2026, 10, 5)))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void getsGroupLists() {
        server.expect(requestTo(BASE + "/api/groups"))
                .andRespond(withSuccess("[\"ИС1-33\",\"СА1-21\"]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/groups/departments"))
                .andRespond(withSuccess("{\"oit\":[\"ИС1-33\"]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/api/groups/department-names"))
                .andRespond(withSuccess("[\"oit\",\"ort\"]", MediaType.APPLICATION_JSON));

        assertThat(client.getAllGroups()).containsExactly("ИС1-33", "СА1-21");
        assertThat(client.getAllGroupsWithDepartments()).containsEntry("oit", java.util.List.of("ИС1-33"));
        assertThat(client.getDepartmentNames()).containsExactly("oit", "ort");
        server.verify();
    }

    @Test
    void findsDepartmentByGroup() {
        server.expect(requestTo(BASE + "/api/groups/find-department?group=%D0%98%D0%A11-33"))
                .andRespond(withSuccess("oit", MediaType.TEXT_PLAIN));

        assertThat(client.getDepartmentsByGroup("ИС1-33")).isEqualTo("oit");
    }
}
