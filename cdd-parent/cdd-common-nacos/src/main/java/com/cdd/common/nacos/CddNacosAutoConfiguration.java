package com.cdd.common.nacos;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@AutoConfiguration
@Profile("nacos")
public class CddNacosAutoConfiguration {

    @Bean
    NacosStartupValidator nacosStartupValidator(
            Environment environment,
            NacosConfigManager configManager,
            NacosConfigProperties configProperties) {
        NacosStartupValidator validator = new NacosStartupValidator(
                environment,
                (dataId, group) -> configManager.getConfigService()
                        .getConfig(dataId, group, configProperties.getTimeout()));
        validator.validate();
        return validator;
    }
}
