package com.cdd.order.config;

import com.cdd.order.support.IdGenerator;
import com.cdd.order.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderServiceConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }
}
