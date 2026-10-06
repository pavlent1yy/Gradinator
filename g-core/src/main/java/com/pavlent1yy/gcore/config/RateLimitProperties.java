package com.pavlent1yy.gcore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "core.rate-limit")
public class RateLimitProperties {

    private Limit auth = new Limit();
    private Limit email = new Limit();
    private Limit general = new Limit();

    @Getter
    @Setter
    public static class Limit {
        private long capacity;
        private long refillMinutes;
    }
}
