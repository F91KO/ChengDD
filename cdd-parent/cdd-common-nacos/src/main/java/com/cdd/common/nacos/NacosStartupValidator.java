package com.cdd.common.nacos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

public final class NacosStartupValidator {

    private static final Set<String> ENVIRONMENTS = Set.of("local", "dev", "test", "prod");
    private static final String NACOS_GROUP = "CHENGDD";
    private static final String SHARED_MARKER = "cdd.nacos.config.shared-data-id";
    private static final String SERVICE_MARKER = "cdd.nacos.config.service-data-id";

    private final Environment environment;
    private final NacosRemoteConfigReader remoteConfigReader;

    NacosStartupValidator(Environment environment, NacosRemoteConfigReader remoteConfigReader) {
        this.environment = environment;
        this.remoteConfigReader = remoteConfigReader;
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
        String content;
        try {
            content = remoteConfigReader.read(dataId, NACOS_GROUP);
        }
        catch (Exception ignored) {
            throw missingRemoteConfiguration(dataId, markerKey);
        }
        if (content == null || content.isBlank() || !hasExpectedMarker(content, dataId, markerKey)) {
            throw missingRemoteConfiguration(dataId, markerKey);
        }
        String importedMarker = environment.getProperty(markerKey, "").trim();
        if (!dataId.equals(importedMarker)) {
            throw missingRemoteConfiguration(dataId, markerKey);
        }
    }

    private boolean hasExpectedMarker(String content, String dataId, String markerKey) {
        List<PropertySource<?>> propertySources;
        try {
            propertySources = new YamlPropertySourceLoader().load(
                    dataId,
                    new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8), dataId));
        }
        catch (IOException | RuntimeException ignored) {
            return false;
        }

        boolean markerFound = false;
        for (PropertySource<?> propertySource : propertySources) {
            Object marker = propertySource.getProperty(markerKey);
            if (marker != null) {
                markerFound = true;
                if (!dataId.equals(marker.toString().trim())) {
                    return false;
                }
            }
        }
        return markerFound;
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

@FunctionalInterface
interface NacosRemoteConfigReader {

    String read(String dataId, String group) throws Exception;
}
