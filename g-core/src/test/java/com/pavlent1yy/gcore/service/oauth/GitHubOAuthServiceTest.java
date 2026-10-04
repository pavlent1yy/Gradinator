package com.pavlent1yy.gcore.service.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubOAuthServiceTest {

    private MockRestServiceServer server;
    private GitHubOAuthService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GitHubOAuthService(builder.build());
    }

    @Test
    void returnsPrimaryEmail() {
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer gh-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andRespond(withSuccess("""
                        [{"email":"second@mail.ru","primary":false},
                         {"email":"main@mail.ru","primary":true}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.getEmail("gh-token")).isEqualTo("main@mail.ru");
        server.verify();
    }

    @Test
    void failsWithoutPrimaryEmail() {
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andRespond(withSuccess("[{\"email\":\"a@mail.ru\",\"primary\":false}]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.getEmail("gh-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GitHub");
    }
}
