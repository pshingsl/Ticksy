package com.Ticksy.backend.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO: 아직 JWT 설정 안 넣음 추후에 넣어서 적용 예정
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Ticksy Swagger")
                .description("Ticksy Swagger UI")
                .version("1.0.0");
    }
}
