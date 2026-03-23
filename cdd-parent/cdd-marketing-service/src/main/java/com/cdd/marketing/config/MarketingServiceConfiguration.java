package com.cdd.marketing.config;

import com.cdd.marketing.support.IdGenerator;
import com.cdd.marketing.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketingServiceConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }
}
