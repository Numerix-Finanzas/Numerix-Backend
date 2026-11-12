package com.upc.numerix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger en http://localhost:8080/swagger-ui.html con el botón Authorize para pegar el JWT
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI numerixOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Numerix API")
                        .description("Cuenta corriente de créditos de pequeños negocios")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
