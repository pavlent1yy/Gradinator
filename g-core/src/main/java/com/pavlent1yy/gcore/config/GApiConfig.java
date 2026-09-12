package com.pavlent1yy.gcore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GApiConfig {

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public RestClient gApiRestClient(
            @Value("${g-api.url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
