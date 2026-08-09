package com.cdd.common.nacos;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class NacosFileModeTest {

    @Test
    void fileModeStartsWithoutImportingFromOrRegisteringWithUnreachableNacos() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(NacosContractApplication.class)
                .profiles("local", "file")
                .web(WebApplicationType.SERVLET)
                .properties(
                        "server.port=0",
                        "spring.application.name=cdd-nacos-file-contract-test",
                        "spring.cloud.nacos.server-addr=127.0.0.1:65534",
                        "spring.cloud.nacos.config.enabled=false",
                        "spring.cloud.nacos.config.import-check.enabled=false",
                        "spring.cloud.nacos.discovery.enabled=false")
                .run()) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.getBeansOfType(NacosServiceRegistry.class)).isEmpty();
        }
    }
}
