package com.cdd.gateway.service;

import com.cdd.gateway.config.GatewayRouteProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayRouteResolverTest {

    private LoadBalancerClient loadBalancerClient;
    private Environment environment;
    private GatewayRouteResolver resolver;

    @BeforeEach
    void setUp() {
        loadBalancerClient = mock(LoadBalancerClient.class);
        environment = mock(Environment.class);
        resolver = new GatewayRouteResolver(loadBalancerClient, environment);
    }

    @Test
    void shouldUseDiscoveredInstanceInNacosProfile() {
        when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
        when(loadBalancerClient.choose("cdd-product-service"))
                .thenReturn(new DefaultServiceInstance("product-1", "cdd-product-service", "10.0.0.8", 8084, false));

        assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
                .isEqualTo("http://10.0.0.8:8084");
    }

    @Test
    void shouldUseStaticUrlOutsideNacosProfile() {
        when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(false);
        GatewayRouteProperties.ServiceRoute route = route("cdd-product-service", "http://127.0.0.1:8084");

        assertThat(resolver.resolveBaseUrl(route)).isEqualTo("http://127.0.0.1:8084");
        verifyNoInteractions(loadBalancerClient);
    }

    @Test
    void shouldFallBackWhenNoInstanceExists() {
        when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
        when(loadBalancerClient.choose("cdd-product-service")).thenReturn(null);

        assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
                .isEqualTo("http://127.0.0.1:8084");
    }

    @Test
    void shouldFallBackWhenLoadBalancerThrows() {
        when(environment.acceptsProfiles(Profiles.of("nacos"))).thenReturn(true);
        when(loadBalancerClient.choose("cdd-product-service"))
                .thenThrow(new IllegalStateException("discovery unavailable"));

        assertThat(resolver.resolveBaseUrl(route("cdd-product-service", "http://127.0.0.1:8084")))
                .isEqualTo("http://127.0.0.1:8084");
    }

    private GatewayRouteProperties.ServiceRoute route(String serviceName, String baseUrl) {
        GatewayRouteProperties.ServiceRoute route = new GatewayRouteProperties.ServiceRoute();
        route.setServiceName(serviceName);
        route.setBaseUrl(baseUrl);
        return route;
    }
}
