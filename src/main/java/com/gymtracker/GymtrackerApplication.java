package com.gymtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.gymtracker")
@EnableJpaRepositories(basePackages = "com.gymtracker.domain.repository")
public class GymtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymtrackerApplication.class, args);
	}

}
