package com.netra.authrex.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ikechukwu.Ezugworie
 * @project rj-utility-service
 * @name SwaggerConfig
 * @date 06/05/2024
 */
@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public OpenAPI usersMicroserviceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        final String securityDomainName = "X-Interswitch-Authorization-Domain";
        return new OpenAPI().addSecurityItem(new SecurityRequirement().addList(securitySchemeName).addList(securityDomainName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                                .addSecuritySchemes(securityDomainName, new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .description("Interswitch Authorization Domain")
                                        .in(SecurityScheme.In.HEADER)
                                        .name(securityDomainName))
                )
                .info(new Info().title("AuthRex Service for Themistra")
                        .description("Authorization Service Swagger API Documentation")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://auth.themistra.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Authorization Service Swagger API Documentation"));
    }
}
