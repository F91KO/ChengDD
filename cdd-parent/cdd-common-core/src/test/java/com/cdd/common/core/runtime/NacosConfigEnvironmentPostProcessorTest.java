package com.cdd.common.core.runtime;

import com.alibaba.nacos.api.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NacosConfigEnvironmentPostProcessorTest {

    @Test
    void shouldSkipWhenConfigModeIsFile() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("cdd.runtime.config-mode", "file")
                .withProperty("spring.application.name", "cdd-gateway");

        new NacosConfigEnvironmentPostProcessor(properties -> configService)
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        verify(configService, never()).getConfig(eq("cdd-common-local.yaml"), eq("CHENGDD"), anyLong());
    }

    @Test
    void shouldLoadSharedAndServiceYamlFromNacos() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        when(configService.getConfig("cdd-common-dev.yaml", "CHENGDD", 3000L)).thenReturn("""
                cdd:
                  sample:
                    common: shared
                    order: common
                """);
        when(configService.getConfig("cdd-gateway-dev.yaml", "CHENGDD", 3000L)).thenReturn("""
                cdd:
                  sample:
                    order: service
                """);

        MockEnvironment environment = new MockEnvironment()
                .withProperty("cdd.runtime.config-mode", "nacos")
                .withProperty("spring.application.name", "cdd-gateway");
        environment.setActiveProfiles("dev");

        new NacosConfigEnvironmentPostProcessor(properties -> configService)
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("cdd.sample.common")).isEqualTo("shared");
        assertThat(environment.getProperty("cdd.sample.order")).isEqualTo("service");
    }
}
