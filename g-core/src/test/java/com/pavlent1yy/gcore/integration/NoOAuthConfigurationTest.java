package com.pavlent1yy.gcore.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.config.location=classpath:application-no-oauth-test.properties")
@AutoConfigureMockMvc
class NoOAuthConfigurationTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithoutOAuthCredentialsAndKeepsEmailAuthenticationAvailable() throws Exception {
        assertThat(context.getBeansOfType(ClientRegistrationRepository.class)).isEmpty();
        mockMvc.perform(post("/core/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"invalid-password\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/core/auth/me")).andExpect(status().isUnauthorized());
    }
}
