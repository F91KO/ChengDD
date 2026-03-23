package com.cdd.decoration.config;

import com.cdd.decoration.support.IdGenerator;
import com.cdd.decoration.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecorationServiceConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }
}
