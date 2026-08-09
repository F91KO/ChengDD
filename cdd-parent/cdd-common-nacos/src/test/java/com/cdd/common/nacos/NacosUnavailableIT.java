package com.cdd.common.nacos;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NacosUnavailableIT {

    @Test
    void startupFailsDuringConfigImportWhenNacosIsUnavailable() {
        AtomicReference<ConfigurableApplicationContext> startedContext = new AtomicReference<>();
        try {
            assertThatThrownBy(() -> startedContext.set(new SpringApplicationBuilder(NacosContractApplication.class)
                    .profiles("local", "nacos")
                    .web(WebApplicationType.SERVLET)
                    .run(
                            "--spring.cloud.nacos.server-addr=127.0.0.1:65534",
                            "--spring.cloud.nacos.discovery.enabled=false")))
                    .hasStackTraceContaining("Nacos Config Data import")
                    .hasStackTraceContaining("cdd-nacos-contract-test-local.yaml")
                    .hasStackTraceContaining("127.0.0.1:65534");
        }
        finally {
            if (startedContext.get() != null) {
                startedContext.get().close();
            }
        }
    }
}
