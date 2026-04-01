package com.cdd.common.core.runtime;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

public class NacosConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME_PREFIX = "nacos-runtime-config:";

    private final Function<Properties, ConfigService> configServiceFactory;
    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    public NacosConfigEnvironmentPostProcessor() {
        this(properties -> {
            try {
                return NacosFactory.createConfigService(properties);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to create nacos config service", ex);
            }
        });
    }

    NacosConfigEnvironmentPostProcessor(Function<Properties, ConfigService> configServiceFactory) {
        this.configServiceFactory = configServiceFactory;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!ConfigMode.NACOS.equals(resolveConfigMode(environment))) {
            return;
        }

        String applicationName = environment.getProperty("spring.application.name");
        String runtimeEnv = resolveRuntimeEnv(environment);
        if (!StringUtils.hasText(applicationName) || !StringUtils.hasText(runtimeEnv)) {
            return;
        }
        if ("cdd-db-migration".equals(applicationName)) {
            return;
        }

        boolean failFast = environment.getProperty("cdd.nacos.fail-fast", Boolean.class, "prod".equalsIgnoreCase(runtimeEnv));
        try {
            ConfigService configService = configServiceFactory.apply(buildNacosProperties(environment, runtimeEnv));
            loadIfPresent(environment, configService, sharedDataId(runtimeEnv));
            loadIfPresent(environment, configService, serviceDataId(applicationName, runtimeEnv));
        } catch (Exception ex) {
            if (failFast) {
                throw new IllegalStateException("Failed to load nacos config for " + applicationName + "@" + runtimeEnv, ex);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private ConfigMode resolveConfigMode(ConfigurableEnvironment environment) {
        return ConfigMode.from(environment.getProperty("cdd.runtime.config-mode", environment.getProperty("CDD_CONFIG_MODE")));
    }

    private String resolveRuntimeEnv(ConfigurableEnvironment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0 && StringUtils.hasText(activeProfiles[0])) {
            return activeProfiles[0];
        }
        String configured = environment.getProperty("cdd.runtime.env", environment.getProperty("CDD_ENV"));
        return StringUtils.hasText(configured) ? configured.trim() : "local";
    }

    private Properties buildNacosProperties(ConfigurableEnvironment environment, String runtimeEnv) {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR,
                environment.getProperty("cdd.nacos.server-addr", environment.getProperty("CDD_NACOS_SERVER_ADDR", "127.0.0.1:8848")));
        String namespace = environment.getProperty("cdd.nacos.namespace",
                environment.getProperty("CDD_NACOS_NAMESPACE", defaultNamespace(runtimeEnv)));
        if (StringUtils.hasText(namespace)) {
            properties.setProperty(PropertyKeyConst.NAMESPACE, namespace.trim());
        }
        String username = environment.getProperty("cdd.nacos.username", environment.getProperty("CDD_NACOS_USERNAME"));
        if (StringUtils.hasText(username)) {
            properties.setProperty(PropertyKeyConst.USERNAME, username.trim());
        }
        String password = environment.getProperty("cdd.nacos.password", environment.getProperty("CDD_NACOS_PASSWORD"));
        if (StringUtils.hasText(password)) {
            properties.setProperty(PropertyKeyConst.PASSWORD, password);
        }
        return properties;
    }

    private void loadIfPresent(ConfigurableEnvironment environment,
                               ConfigService configService,
                               String dataId) throws Exception {
        String group = environment.getProperty("cdd.nacos.group", environment.getProperty("CDD_NACOS_GROUP", "CHENGDD"));
        long timeoutMs = environment.getProperty("cdd.nacos.timeout-ms", Long.class, 3000L);
        String content = configService.getConfig(dataId, group, timeoutMs);
        if (!StringUtils.hasText(content)) {
            return;
        }
        List<PropertySource<?>> propertySources = yamlLoader.load(
                PROPERTY_SOURCE_NAME_PREFIX + dataId,
                new NamedByteArrayResource(content.getBytes(StandardCharsets.UTF_8), dataId));
        for (PropertySource<?> propertySource : propertySources) {
            environment.getPropertySources().addFirst(propertySource);
        }
    }

    private String defaultNamespace(String runtimeEnv) {
        return "local".equalsIgnoreCase(runtimeEnv) ? "" : "chengdd-" + runtimeEnv;
    }

    private String sharedDataId(String runtimeEnv) {
        return "cdd-common-" + runtimeEnv + ".yaml";
    }

    private String serviceDataId(String applicationName, String runtimeEnv) {
        return applicationName + "-" + runtimeEnv + ".yaml";
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
