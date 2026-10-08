package com.godoy.corebanking;

import org.springframework.boot.SpringApplication;

public class TestCoreBankingApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoreBankingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
