package com.cdd.report.config;

import com.cdd.report.support.IdGenerator;
import com.cdd.report.support.TimeBasedIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportServiceConfiguration {

    @Bean
    public IdGenerator idGenerator() {
        return new TimeBasedIdGenerator();
    }
}
