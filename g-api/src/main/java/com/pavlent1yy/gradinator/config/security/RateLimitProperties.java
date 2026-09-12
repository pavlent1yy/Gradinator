package com.pavlent1yy.gradinator.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gradinator.rate-limit")
public class RateLimitProperties {

    private Limit schedule = new Limit();
    private Limit scheduleAll = new Limit();
    private Limit user = new Limit();
    private Limit admin = new Limit();

    @Getter
    @Setter
    public static class Limit {
        private long capacity;
        private long refillMinutes;
    }
}