package com.smartblog.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Smart Blogging Platform}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Blogging Platform API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for Smart Blogging Platform with:
                                - User Management with role-based access
                                - Post Management with full-text search
                                - Comment System with polyglot persistence
                                - Tag Management
                                - Comprehensive validation and error handling
                                """)
                        .contact(new Contact()
                                .name("Smart Blog Team")
                                .email("support@smartblog.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development"),
                        new Server()
                                .url("https://api.smartblog.com")
                                .description("Production")
                ));
    }
}