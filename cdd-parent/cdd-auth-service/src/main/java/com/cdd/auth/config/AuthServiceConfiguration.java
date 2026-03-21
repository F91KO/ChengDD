package com.cdd.auth.config;

import com.cdd.auth.infrastructure.persistence.AuthAccountRepository;
import com.cdd.auth.infrastructure.persistence.RefreshTokenSessionRepository;
import com.cdd.auth.service.RefreshTokenSessionStore;
import com.cdd.auth.service.RuntimeAccountService;
import com.cdd.auth.support.IdGenerator;
import com.cdd.auth.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthServiceConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }

    @Bean
    public RuntimeAccountService runtimeAccountService(AuthAccountRepository authAccountRepository,
                                                       PasswordEncoder passwordEncoder) {
        return new RuntimeAccountService(authAccountRepository, passwordEncoder);
    }

    @Bean
    public RefreshTokenSessionStore refreshTokenSessionStore(RefreshTokenSessionRepository refreshTokenSessionRepository,
                                                             IdGenerator idGenerator) {
        return new RefreshTokenSessionStore(refreshTokenSessionRepository, idGenerator);
    }
}
