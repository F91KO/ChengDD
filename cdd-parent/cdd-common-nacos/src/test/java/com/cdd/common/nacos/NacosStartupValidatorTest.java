package com.cdd.common.nacos;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
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

    @Test
    void shouldRequireSharedMarkerFromExpectedNacosDataId() {
        MockEnvironment environment = validEnvironment("local");
        environment.getPropertySources().remove("CHENGDD@cdd-common-local.yaml");

        assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-common-local.yaml")
                .hasMessageContaining("remote configuration");
    }

    @Test
    void shouldRejectServiceMarkerThatDoesNotMatchApplicationDataId() {
        MockEnvironment environment = validEnvironment("local");
        environment.getPropertySources().replace(
                "CHENGDD@cdd-contract-service-local.yaml",
                nacosPropertySource(
                        "cdd-contract-service-local.yaml",
                        "cdd.nacos.config.service-data-id",
                        "cdd-other-service-local.yaml"));

        assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-contract-service-local.yaml")
                .hasMessageContaining("marker");
    }

    @Test
    void shouldNotAcceptMarkersFromLocalPropertySources() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "cdd-contract-service")
                .withProperty("cdd.runtime.env", "local")
                .withProperty("spring.cloud.nacos.server-addr", "127.0.0.1:8848")
                .withProperty("spring.cloud.nacos.config.group", "CHENGDD")
                .withProperty("spring.cloud.nacos.discovery.group", "CHENGDD")
                .withProperty("cdd.nacos.config.shared-data-id", "cdd-common-local.yaml")
                .withProperty("cdd.nacos.config.service-data-id", "cdd-contract-service-local.yaml");

        assertThatThrownBy(() -> new NacosStartupValidator(environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-common-local.yaml")
                .hasMessageContaining("remote configuration");
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
        environment.getPropertySources().addFirst(nacosPropertySource(
                "cdd-contract-service-" + runtimeEnv + ".yaml",
                "cdd.nacos.config.service-data-id",
                "cdd-contract-service-" + runtimeEnv + ".yaml"));
        environment.getPropertySources().addFirst(nacosPropertySource(
                "cdd-common-" + runtimeEnv + ".yaml",
                "cdd.nacos.config.shared-data-id",
                "cdd-common-" + runtimeEnv + ".yaml"));
        return environment;
    }

    private static MapPropertySource nacosPropertySource(String dataId, String markerKey, String markerValue) {
        return new MapPropertySource("CHENGDD@" + dataId, Map.of(markerKey, markerValue));
    }
}
