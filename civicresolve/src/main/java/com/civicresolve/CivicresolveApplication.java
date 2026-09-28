package com.civicresolve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CivicresolveApplication {

	public static void main(String[] args) {
		SpringApplication.run(CivicresolveApplication.class, args);
	}

}
