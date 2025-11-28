package com.example_api.epc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EpcApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpcApplication.class, args);
	}

}
