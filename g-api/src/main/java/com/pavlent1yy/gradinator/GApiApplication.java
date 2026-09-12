package com.pavlent1yy.gradinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication(
		exclude = UserDetailsServiceAutoConfiguration.class
)
public class GApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(GApiApplication.class, args);
	}

}
