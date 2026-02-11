package com.smartblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * EPIC 1: Spring Boot 3.x Main Application Entry Point
 * Replaces JavaFX App.java with REST/GraphQL web application
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.smartblog.infrastructure.repository.jpa")
public class SmartBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBlogApplication.class, args);
    }
}
