package com.pavlent1yy.gcore.service.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private final RestClient restClient;

    public String getEmail(String accessToken) {

        List<Map<String, Object>> emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .header(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                .map(email -> (String) email.get("email"))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "GitHub не предоставил email пользователя"
                        )
                );
    }
}