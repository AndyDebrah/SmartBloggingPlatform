package com.smartblog.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;

@Configuration
@Profile("test")
public class TestOAuth2ClientProperties {

    @Bean
    @Primary
    public OAuth2ClientProperties oauth2ClientProperties() {
        // Return a subclass that disables the default validation in afterPropertiesSet
        return new OAuth2ClientProperties() {
            @Override
            public void afterPropertiesSet() {
                // no-op to avoid 'Client id must not be empty' validation during test profile
            }
        };
    }

}
