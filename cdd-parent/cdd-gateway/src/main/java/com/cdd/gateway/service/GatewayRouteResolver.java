package com.cdd.gateway.service;

import com.cdd.gateway.config.GatewayRouteProperties.ServiceRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewayRouteResolver {

    private static final Logger log = LoggerFactory.getLogger(GatewayRouteResolver.class);

    private final LoadBalancerClient loadBalancerClient;
    private final Environment environment;

    public GatewayRouteResolver(LoadBalancerClient loadBalancerClient, Environment environment) {
        this.loadBalancerClient = loadBalancerClient;
        this.environment = environment;
    }

    public String resolveBaseUrl(ServiceRoute serviceRoute) {
        if (!environment.acceptsProfiles(Profiles.of("nacos"))
                || !StringUtils.hasText(serviceRoute.getServiceName())) {
            return serviceRoute.getBaseUrl();
        }

        try {
            ServiceInstance instance = loadBalancerClient.choose(serviceRoute.getServiceName());
            if (instance != null) {
                return instance.getUri().toString();
            }
            log.warn("No discovered instance for service '{}'; using static gateway route URL", serviceRoute.getServiceName());
        } catch (RuntimeException ex) {
            log.warn("Unable to resolve service '{}' through load balancer; using static gateway route URL",
                    serviceRoute.getServiceName(), ex);
        }
        return serviceRoute.getBaseUrl();
    }
}
