package com.cdd.common.nacos;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class NacosConfigImportIT {

    @Test
    void importsSharedAndServiceConfigurationAndPublishesASelectableInstance() {
        ConfigurableApplicationContext context = null;
        try {
            context = new SpringApplicationBuilder(NacosContractApplication.class)
                    .profiles("local", "nacos")
                    .web(WebApplicationType.SERVLET)
                    .run();

            Environment environment = context.getEnvironment();
            DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
            LoadBalancerClient loadBalancerClient = context.getBean(LoadBalancerClient.class);

            assertThat(environment.getProperty("cdd.contract.shared-only")).isEqualTo("from-common");
            assertThat(environment.getProperty("cdd.contract.precedence")).isEqualTo("from-service");
            assertThat(environment.getProperty("cdd.contract.service-only")).isEqualTo("from-service");
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                    assertThat(discoveryClient.getInstances("cdd-nacos-contract-test")).isNotEmpty());
            ServiceInstance selected = loadBalancerClient.choose("cdd-nacos-contract-test");
            assertThat(selected).isNotNull();
        }
        finally {
            if (context != null) {
                context.close();
            }
        }
    }
}
