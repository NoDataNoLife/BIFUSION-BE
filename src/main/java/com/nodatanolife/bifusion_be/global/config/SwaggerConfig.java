package com.nodatanolife.bifusion_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String COOKIE_AUTH = "cookieAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme cookieScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("access_token");

        return new OpenAPI()
                .info(new Info()
                        .title("BIFUSION API")
                        .description("BIFUSION 프로젝트 API 문서")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(COOKIE_AUTH, cookieScheme))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
    }
}
