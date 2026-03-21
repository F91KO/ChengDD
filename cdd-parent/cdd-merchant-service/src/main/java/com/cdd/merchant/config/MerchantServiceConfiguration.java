package com.cdd.merchant.config;

import com.cdd.merchant.support.IdGenerator;
import com.cdd.merchant.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantServiceConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }
}
