package com.godoy.corebanking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CoreBankingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreBankingApiApplication.class, args);
	}

}
