package com.pavlent1yy.gcore.integration;

import com.pavlent1yy.gcore.client.GApiClient;
import com.pavlent1yy.gcore.client.GApiProxy;
import com.pavlent1yy.gcore.controller.GApiProxyController;
import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.Role;
import com.pavlent1yy.gcore.service.jwt.JwtService;
import com.pavlent1yy.gcore.config.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private GApiProxy gApiProxy;

    @MockitoBean
    private GApiClient gApiClient;

    @BeforeEach
    void setUp() {
        when(gApiProxy.get(anyString(), any())).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[\"ok\"]".getBytes(StandardCharsets.UTF_8)));
        when(gApiClient.getAllGroups()).thenReturn(List.of("ИС1-43"));
    }

    @Test
    void everyProxiedGApiPathIsPublic() throws Exception {
        for (String path : GApiProxyController.PUBLIC_PATHS) {
            mockMvc.perform(get(path).param("group", "ИС1-43"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("[\"ok\"]"));
        }
    }

    @Test
    void adminAndUnknownPathsAreNotProxied() throws Exception {
        mockMvc.perform(get("/admin/snapshots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/snapshots")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/teachers")).andExpect(status().isUnauthorized());
    }

    @Test
    void coreSchedulePassesGApiErrorsInsteadOf401() throws Exception {
        when(gApiClient.getSchedule("ИС1-43", LocalDate.of(2026, 9, 28)))
                .thenThrow(new ScheduleNotFoundException("Нет актуальных данных"));
        when(gApiClient.getSchedule("ИС1-43", LocalDate.of(2020, 1, 1)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
                        "{\"error\":\"Дата находится вне допустимого диапазона\"}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));
        when(gApiClient.getSchedule("ИС1-43", LocalDate.of(2026, 9, 29)))
                .thenThrow(new ResourceAccessException("timeout"));

        mockMvc.perform(get("/core/schedule").param("group", "ИС1-43").param("date", "2026-09-28"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Нет актуальных данных"));
        mockMvc.perform(get("/core/schedule").param("group", "ИС1-43").param("date", "2020-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Дата находится вне допустимого диапазона"));
        mockMvc.perform(get("/core/schedule").param("group", "ИС1-43").param("date", "2026-09-29"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void badRequestOnPublicPathIsNot401() throws Exception {
        mockMvc.perform(get("/core/schedule").param("group", "ИС1-43").param("date", "не-дата"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/core/schedule").param("date", "2026-09-28"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void brokenOrStaleCookieDoesNotBreakPublicPaths() throws Exception {
        User deleted = new User();
        deleted.setEmail("deleted@mail.ru");
        deleted.setRole(Role.STUDENT);
        deleted.setEnabled(true);
        String staleToken = jwtService.generateToken(new UserDetailsImpl(deleted));

        mockMvc.perform(get("/teachers").cookie(new Cookie("gradinator_access", "garbage")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/teachers").cookie(new Cookie("gradinator_access", staleToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/core/schedule/groups").cookie(new Cookie("gradinator_access", staleToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ИС1-43")));
    }
}
