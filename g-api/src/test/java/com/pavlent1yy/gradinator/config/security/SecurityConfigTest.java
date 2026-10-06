package com.pavlent1yy.gradinator.config.security;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, StubController.class})
    static class TestConfig {
    }

    @RestController
    static class StubController {
        @GetMapping({"/api/schedule", "/api/schedule/today", "/api/groups", "/api/groups/departments",
                "/api/teachers", "/api/subjects", "/api/rooms", "/api/admin/snapshots", "/api/secret", "/error"})
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicEndpointsAreOpen() throws Exception {
        for (String path : new String[]{"/api/schedule", "/api/schedule/today", "/api/groups",
                "/api/groups/departments", "/api/teachers", "/api/subjects", "/api/rooms"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mvc.perform(get("/api/admin/snapshots")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/admin/snapshots").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/snapshots").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEndpointsAreDeniedEvenForAdmin() throws Exception {
        mvc.perform(get("/api/secret").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityHeadersAreSet() throws Exception {
        mvc.perform(get("/api/groups"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void errorPageIsOpenSoClientErrorsAreNotTurnedInto403() throws Exception {
        mvc.perform(get("/error")).andExpect(status().isOk());
        mvc.perform(get("/api/secret").with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            return request;
        })).andExpect(status().isOk());
    }
}
