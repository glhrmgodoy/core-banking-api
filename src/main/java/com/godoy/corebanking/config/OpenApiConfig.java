package com.godoy.corebanking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI coreBankingOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Core Banking API")
						.description("API de core bancário com contas, ledger e transferências PIX")
						.version("v1"));
	}

}
