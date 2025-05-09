package com.dearwith.dearwith_backend;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dearwithOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DearWith API")
                        .version("v1")
                        .description("DearWith Backend API 명세")
                );
    }
}