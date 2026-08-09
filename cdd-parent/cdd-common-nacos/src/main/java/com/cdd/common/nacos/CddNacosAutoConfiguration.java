package com.cdd.common.nacos;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@AutoConfiguration
@Profile("nacos")
public class CddNacosAutoConfiguration {

    @Bean
    NacosStartupValidator nacosStartupValidator(Environment environment) {
        NacosStartupValidator validator = new NacosStartupValidator(environment);
        validator.validate();
        return validator;
    }
}
