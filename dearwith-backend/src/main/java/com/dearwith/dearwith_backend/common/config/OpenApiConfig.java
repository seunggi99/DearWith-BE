package com.dearwith.dearwith_backend.common.config;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "/", description = "Current origin")
        }
)
public class OpenApiConfig {
    @Bean
    public OpenAPI dearwithOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DearWith API")
                        .version("v1")
                        .description("DearWith Backend API 명세"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")
                        .addList("refreshToken"))

                .components(new Components()
                        // Access Token
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Access Token (Authorization: Bearer {token})")
                        )
                        // Refresh Token
                        .addSecuritySchemes("refreshToken",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Refresh-Token")
                                        .description("Refresh Token (X-Refresh-Token 헤더)")
                        )
                );
    }
}