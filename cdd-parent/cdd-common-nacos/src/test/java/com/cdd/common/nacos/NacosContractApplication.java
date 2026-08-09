package com.cdd.common.nacos;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class NacosContractApplication {

    @Bean
    @Profile("nacos")
    ContractServiceConfiguration contractServiceConfiguration(Environment environment) {
        String serviceOnly = environment.getProperty("cdd.contract.service-only");
        if (!StringUtils.hasText(serviceOnly)) {
            String dataId = environment.getRequiredProperty("spring.application.name") + "-local.yaml";
            String serverAddr = environment.getRequiredProperty("spring.cloud.nacos.server-addr");
            throw new IllegalStateException(
                    "Nacos Config Data import did not provide required service configuration "
                            + dataId + " from " + serverAddr);
        }
        return new ContractServiceConfiguration(serviceOnly);
    }

    record ContractServiceConfiguration(String serviceOnly) {
    }
}
