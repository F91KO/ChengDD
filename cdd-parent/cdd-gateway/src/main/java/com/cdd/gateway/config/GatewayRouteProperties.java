package com.cdd.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cdd.gateway.routes")
public class GatewayRouteProperties {

    private final ServiceRoute auth = new ServiceRoute();
    private final ServiceRoute report = new ServiceRoute();
    private final ServiceRoute config = new ServiceRoute();
    private final ServiceRoute product = new ServiceRoute();
    private final ServiceRoute order = new ServiceRoute();
    private final ServiceRoute release = new ServiceRoute();

    public ServiceRoute getAuth() {
        return auth;
    }

    public ServiceRoute getReport() {
        return report;
    }

    public ServiceRoute getConfig() {
        return config;
    }

    public ServiceRoute getProduct() {
        return product;
    }

    public ServiceRoute getOrder() {
        return order;
    }

    public ServiceRoute getRelease() {
        return release;
    }

    public static class ServiceRoute {

        @NotBlank
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl == null
                    ? null
                    : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        }
    }
}
