package com.cdd.common.nacos;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NacosStartupValidatorTest {

    @Test
    void shouldAcceptLocalPublicNamespace() {
        MockEnvironment environment = validEnvironment("local");
        assertDoesNotThrow(() -> new NacosStartupValidator(environment).validate());
    }

    @Test
    void shouldRequireNamespaceIdOutsideLocal() {
        MockEnvironment environment = validEnvironment("dev")
                .withProperty("spring.cloud.nacos.config.namespace", "")
                .withProperty("spring.cloud.nacos.discovery.namespace", "");
        assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace ID");
    }

    @Test
    void shouldRequireMatchingNonBlankGroups() {
        MockEnvironment environment = validEnvironment("local")
                .withProperty("spring.cloud.nacos.discovery.group", "");
        assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHENGDD");
    }

    private static MockEnvironment validEnvironment(String runtimeEnv) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "cdd-contract-service")
                .withProperty("cdd.runtime.env", runtimeEnv)
                .withProperty("spring.cloud.nacos.server-addr", "127.0.0.1:8848")
                .withProperty("spring.cloud.nacos.config.group", "CHENGDD")
                .withProperty("spring.cloud.nacos.discovery.group", "CHENGDD");
        if (!"local".equals(runtimeEnv)) {
            environment.withProperty("spring.cloud.nacos.config.namespace", "namespace-id-" + runtimeEnv)
                    .withProperty("spring.cloud.nacos.discovery.namespace", "namespace-id-" + runtimeEnv);
        }
        return environment;
    }
}
