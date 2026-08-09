package com.cdd.common.nacos;

import java.util.LinkedHashMap;
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
        assertDoesNotThrow(() -> validator(environment, validRemoteConfigs("local")).validate());
    }

    @Test
    void shouldRequireNamespaceIdOutsideLocal() {
        MockEnvironment environment = validEnvironment("dev")
                .withProperty("spring.cloud.nacos.config.namespace", "")
                .withProperty("spring.cloud.nacos.discovery.namespace", "");
        assertThatThrownBy(() -> validator(environment, validRemoteConfigs("dev")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("namespace ID");
    }

    @Test
    void shouldRequireMatchingNonBlankGroups() {
        MockEnvironment environment = validEnvironment("local")
                .withProperty("spring.cloud.nacos.discovery.group", "");
        assertThatThrownBy(() -> validator(environment, validRemoteConfigs("local")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHENGDD");
    }

    @Test
    void shouldRequireSharedMarkerFromExpectedNacosDataId() {
        MockEnvironment environment = validEnvironment("local");
        Map<String, String> remoteConfigs = Map.of(
                "cdd-contract-service-local.yaml",
                serviceConfig("cdd-contract-service-local.yaml"));

        assertThatThrownBy(() -> validator(environment, remoteConfigs).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-common-local.yaml")
                .hasMessageContaining("remote configuration");
    }

    @Test
    void shouldRejectServiceMarkerThatDoesNotMatchApplicationDataId() {
        MockEnvironment environment = validEnvironment("local");
        Map<String, String> remoteConfigs = new LinkedHashMap<>(validRemoteConfigs("local"));
        remoteConfigs.put(
                "cdd-contract-service-local.yaml",
                serviceConfig("cdd-other-service-local.yaml"));

        assertThatThrownBy(() -> validator(environment, remoteConfigs).validate())
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

        assertThatThrownBy(() -> validator(environment, Map.of()).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-common-local.yaml")
                .hasMessageContaining("remote configuration");
    }

    @Test
    void shouldNotAcceptLocalMapPropertySourcesThatSpoofNacosNames() {
        MockEnvironment environment = baseEnvironment("local");
        environment.getPropertySources().addFirst(new MapPropertySource(
                "CHENGDD@cdd-contract-service-local.yaml",
                Map.of("cdd.nacos.config.service-data-id", "cdd-contract-service-local.yaml")));
        environment.getPropertySources().addFirst(new MapPropertySource(
                "CHENGDD@cdd-common-local.yaml",
                Map.of("cdd.nacos.config.shared-data-id", "cdd-common-local.yaml")));

        assertThatThrownBy(() -> validator(environment, Map.of()).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cdd-common-local.yaml")
                .hasMessageContaining("remote configuration");
    }

    private static MockEnvironment validEnvironment(String runtimeEnv) {
        return baseEnvironment(runtimeEnv)
                .withProperty(
                        "cdd.nacos.config.shared-data-id",
                        "cdd-common-" + runtimeEnv + ".yaml")
                .withProperty(
                        "cdd.nacos.config.service-data-id",
                        "cdd-contract-service-" + runtimeEnv + ".yaml");
    }

    private static MockEnvironment baseEnvironment(String runtimeEnv) {
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

    private static NacosStartupValidator validator(
            MockEnvironment environment, Map<String, String> remoteConfigs) {
        return new NacosStartupValidator(environment, (dataId, group) -> remoteConfigs.get(dataId));
    }

    private static Map<String, String> validRemoteConfigs(String runtimeEnv) {
        String sharedDataId = "cdd-common-" + runtimeEnv + ".yaml";
        String serviceDataId = "cdd-contract-service-" + runtimeEnv + ".yaml";
        return Map.of(
                sharedDataId, sharedConfig(sharedDataId),
                serviceDataId, serviceConfig(serviceDataId));
    }

    private static String sharedConfig(String markerValue) {
        return "cdd:\n  nacos:\n    config:\n      shared-data-id: " + markerValue + "\n";
    }

    private static String serviceConfig(String markerValue) {
        return "cdd:\n  nacos:\n    config:\n      service-data-id: " + markerValue + "\n";
    }
}
