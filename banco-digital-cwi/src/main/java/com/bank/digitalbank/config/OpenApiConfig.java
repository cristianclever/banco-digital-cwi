package com.bank.digitalbank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Bank API")
                        .version("1.0.0")
                        .description("API REST simplificada para simulação de transferências bancárias sob cenários de alta concorrência.")
                        .contact(new Contact()
                                .name("Cristian Oliveira")
                                .email("cristian@email.com")));
    }
}