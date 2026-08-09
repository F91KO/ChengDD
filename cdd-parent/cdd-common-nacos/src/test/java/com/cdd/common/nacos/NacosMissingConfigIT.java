package com.cdd.common.nacos;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NacosMissingConfigIT {

    @Test
    void startupFailsWhenRequiredServiceConfigurationIsMissing() {
        AtomicReference<ConfigurableApplicationContext> startedContext = new AtomicReference<>();
        try {
            assertThatThrownBy(() -> startedContext.set(new SpringApplicationBuilder(NacosContractApplication.class)
                    .profiles("local", "nacos")
                    .web(WebApplicationType.SERVLET)
                    .run("--spring.application.name=cdd-nacos-missing-contract-test")))
                    .hasStackTraceContaining("cdd-nacos-missing-contract-test-local.yaml");
        }
        finally {
            if (startedContext.get() != null) {
                startedContext.get().close();
            }
        }
    }
}
