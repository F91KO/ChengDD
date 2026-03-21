package com.cdd.auth.config;

import com.cdd.auth.service.RefreshTokenSessionStore;
import com.cdd.auth.service.RuntimeAccountService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthServiceProperties.class)
public class AuthServiceConfiguration {

    @Bean
    public RuntimeAccountService runtimeAccountService(AuthServiceProperties authServiceProperties) {
        return new RuntimeAccountService(authServiceProperties);
    }

    @Bean
    public RefreshTokenSessionStore refreshTokenSessionStore() {
        return new RefreshTokenSessionStore();
    }
}
