package com.joe_bor.svt_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SvtApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvtApiApplication.class, args);
	}

}
