package com.cyclehaven.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Generates live API documentation at /swagger-ui.html.
 *
 * <p>Worth having beyond the convenience: it produces a browsable, executable
 * description of every endpoint straight from the code, so the API can be
 * demonstrated without a frontend and can never drift out of date the way a
 * hand-written document would.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cycleHavenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cycle Haven API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Cycle Haven bicycle store.

                                Obtain a token from POST /api/auth/login, then click
                                Authorize and paste it to call protected endpoints.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
