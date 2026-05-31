package com.pavlent1yy.gradinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GradinatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GradinatorApplication.class, args);
		ScheduleWebParserService service = new ScheduleWebParserService();

		System.out.println("-------------\n" + service.getChanges("СТ1-25") + "\n-----------\n\n");
	}

}
