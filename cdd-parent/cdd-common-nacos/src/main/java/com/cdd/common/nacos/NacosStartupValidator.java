package com.cdd.common.nacos;

import java.util.Set;

import org.springframework.core.env.Environment;

public final class NacosStartupValidator {

    private static final Set<String> ENVIRONMENTS = Set.of("local", "dev", "test", "prod");

    private final Environment environment;

    public NacosStartupValidator(Environment environment) {
        this.environment = environment;
    }

    public void validate() {
        requireText("spring.application.name");
        requireText("spring.cloud.nacos.server-addr");

        String runtimeEnv = environment.getProperty(
                "cdd.runtime.env",
                environment.getProperty("CDD_ENV", "local"));
        if (!ENVIRONMENTS.contains(runtimeEnv)) {
            throw new IllegalStateException("Unsupported cdd.runtime.env: " + runtimeEnv);
        }

        String configGroup = environment.getProperty("spring.cloud.nacos.config.group", "").trim();
        String discoveryGroup = environment.getProperty("spring.cloud.nacos.discovery.group", "").trim();
        if (!"CHENGDD".equals(configGroup) || !configGroup.equals(discoveryGroup)) {
            throw new IllegalStateException("Nacos Config and Discovery group must both be CHENGDD");
        }

        String configNamespace = environment.getProperty("spring.cloud.nacos.config.namespace", "").trim();
        String discoveryNamespace = environment.getProperty("spring.cloud.nacos.discovery.namespace", "").trim();
        if (!"local".equals(runtimeEnv)
                && (configNamespace.isEmpty() || !configNamespace.equals(discoveryNamespace))) {
            throw new IllegalStateException("Non-local Nacos Config and Discovery require the same namespace ID");
        }
    }

    private String requireText(String key) {
        String value = environment.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Required Nacos property is blank: " + key);
        }
        return value;
    }
}
