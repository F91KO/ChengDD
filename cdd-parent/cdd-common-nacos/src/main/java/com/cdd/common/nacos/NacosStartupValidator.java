package com.cdd.common.nacos;

import java.util.Set;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

public final class NacosStartupValidator {

    private static final Set<String> ENVIRONMENTS = Set.of("local", "dev", "test", "prod");
    private static final String NACOS_GROUP = "CHENGDD";
    private static final String SHARED_MARKER = "cdd.nacos.config.shared-data-id";
    private static final String SERVICE_MARKER = "cdd.nacos.config.service-data-id";

    private final Environment environment;

    public NacosStartupValidator(Environment environment) {
        this.environment = environment;
    }

    public void validate() {
        String applicationName = requireText("spring.application.name");
        requireText("spring.cloud.nacos.server-addr");

        String runtimeEnv = environment.getProperty(
                "cdd.runtime.env",
                environment.getProperty("CDD_ENV", "local"));
        if (!ENVIRONMENTS.contains(runtimeEnv)) {
            throw new IllegalStateException("Unsupported cdd.runtime.env: " + runtimeEnv);
        }

        String configGroup = environment.getProperty("spring.cloud.nacos.config.group", "").trim();
        String discoveryGroup = environment.getProperty("spring.cloud.nacos.discovery.group", "").trim();
        if (!NACOS_GROUP.equals(configGroup) || !configGroup.equals(discoveryGroup)) {
            throw new IllegalStateException("Nacos Config and Discovery group must both be CHENGDD");
        }

        String configNamespace = environment.getProperty("spring.cloud.nacos.config.namespace", "").trim();
        String discoveryNamespace = environment.getProperty("spring.cloud.nacos.discovery.namespace", "").trim();
        if (!"local".equals(runtimeEnv)
                && (configNamespace.isEmpty() || !configNamespace.equals(discoveryNamespace))) {
            throw new IllegalStateException("Non-local Nacos Config and Discovery require the same namespace ID");
        }

        requireRemoteMarker("cdd-common-" + runtimeEnv + ".yaml", SHARED_MARKER);
        requireRemoteMarker(applicationName + "-" + runtimeEnv + ".yaml", SERVICE_MARKER);
    }

    private void requireRemoteMarker(String dataId, String markerKey) {
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            throw missingRemoteConfiguration(dataId, markerKey);
        }

        PropertySource<?> propertySource = configurableEnvironment.getPropertySources()
                .get(NACOS_GROUP + "@" + dataId);
        Object marker = propertySource == null ? null : propertySource.getProperty(markerKey);
        if (marker == null || !dataId.equals(marker.toString().trim())) {
            throw missingRemoteConfiguration(dataId, markerKey);
        }
    }

    private IllegalStateException missingRemoteConfiguration(String dataId, String markerKey) {
        return new IllegalStateException(
                "Required Nacos remote configuration was not loaded or its marker is invalid: dataId="
                        + dataId + ", group=" + NACOS_GROUP + ", marker=" + markerKey);
    }

    private String requireText(String key) {
        String value = environment.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Required Nacos property is blank: " + key);
        }
        return value;
    }
}
