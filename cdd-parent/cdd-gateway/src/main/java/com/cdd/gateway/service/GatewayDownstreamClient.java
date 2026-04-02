package com.cdd.gateway.service;

import com.cdd.common.core.context.RequestHeaders;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.common.web.context.RequestIdHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GatewayDownstreamClient {

    private static final List<String> REQUEST_HEADER_BLACKLIST = List.of(
            HttpHeaders.HOST,
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.TRANSFER_ENCODING);

    private static final List<String> RESPONSE_HEADER_BLACKLIST = List.of(
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.TRANSFER_ENCODING);
    private static final Pattern PRODUCT_CATEGORY_ID_PATH = Pattern.compile("^/api/product/categories/[^/]+$");
    private static final Pattern PRODUCT_SPU_ID_PATH = Pattern.compile("^/api/product/spu/[^/]+$");
    private static final Pattern ORDER_ACTION_PATH = Pattern.compile("^/api/order/orders/[^/]+/(ship|cancel)$");
    private static final Pattern ORDER_AFTER_SALE_ACTION_PATH = Pattern.compile("^/api/order/after-sales/[^/]+/(review|return)$");
    private static final Pattern CONFIG_SWITCH_CHANGE_PATH = Pattern.compile("^/api/config/merchant/feature-switches/[^/]+/change$");

    private final RestTemplate gatewayProxyRestTemplate;
    private final ObjectMapper objectMapper;

    public GatewayDownstreamClient(RestTemplate gatewayProxyRestTemplate, ObjectMapper objectMapper) {
        this.gatewayProxyRestTemplate = gatewayProxyRestTemplate;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body, String baseUrl) {
        DownstreamRequest downstreamRequest = buildDownstreamRequest(request, body, baseUrl);
        return exchange(request.getMethod(), downstreamRequest.targetUri(), copyRequestHeaders(request), downstreamRequest.body());
    }

    public ResponseEntity<byte[]> get(String baseUrl, String pathWithQuery, HttpServletRequest request) {
        return exchange(HttpMethod.GET.name(), URI.create(baseUrl + pathWithQuery), copyRequestHeaders(request), null);
    }

    private ResponseEntity<byte[]> exchange(String method, URI targetUri, HttpHeaders headers, byte[] body) {
        HttpEntity<byte[]> entity = new HttpEntity<>(body != null && body.length > 0 ? body : null, headers);
        try {
            ResponseEntity<byte[]> response = gatewayProxyRestTemplate.exchange(
                    targetUri,
                    HttpMethod.valueOf(method),
                    entity,
                    byte[].class);
            return ResponseEntity.status(response.getStatusCode())
                    .headers(filterResponseHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(filterResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsByteArray());
        } catch (ResourceAccessException ex) {
            return gatewayUnavailable();
        }
    }

    private DownstreamRequest buildDownstreamRequest(HttpServletRequest request, byte[] body, String baseUrl) {
        String path = resolveRequestPath(request);
        TenantBinding tenantBinding = TenantBinding.from(AuthContextHolder.get());
        return new DownstreamRequest(
                buildTargetUri(baseUrl, request.getMethod(), path, request.getQueryString(), tenantBinding),
                rewriteRequestBodyIfNecessary(request, body, path, tenantBinding));
    }

    private URI buildTargetUri(String baseUrl, String method, String path, String query, TenantBinding tenantBinding) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path);
        if (StringUtils.hasText(query)) {
            builder.query(query);
        }
        if (HttpMethod.GET.matches(method)) {
            applyTenantQueryParams(builder, path, tenantBinding);
        }
        return builder.build(true).toUri();
    }

    private void applyTenantQueryParams(UriComponentsBuilder builder, String path, TenantBinding tenantBinding) {
        if (tenantBinding == null) {
            return;
        }
        if (isConfigMerchantQueryPath(path)) {
            builder.replaceQueryParam("merchant_id", tenantBinding.merchantText());
            if (path.startsWith("/api/config/publish-records") && StringUtils.hasText(tenantBinding.storeText())) {
                builder.replaceQueryParam("store_id", tenantBinding.storeText());
            }
            return;
        }
        if (isNumericTenantQueryPath(path)) {
            builder.replaceQueryParam("merchant_id", tenantBinding.merchantNumericText());
            builder.replaceQueryParam("store_id", tenantBinding.storeNumericText());
        }
    }

    private byte[] rewriteRequestBodyIfNecessary(HttpServletRequest request,
                                                 byte[] body,
                                                 String path,
                                                 TenantBinding tenantBinding) {
        if (body == null || body.length == 0 || tenantBinding == null || !isJsonRequest(request)) {
            return body;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            if (!(rootNode instanceof ObjectNode objectNode)) {
                return body;
            }
            boolean mutated = false;
            if (isConfigMerchantBodyPath(path)) {
                mutated |= putTextIfPresent(objectNode, "merchant_id", tenantBinding.merchantText());
                if ("/api/config/publish-records".equals(path)) {
                    mutated |= putTextIfPresent(objectNode, "store_id", tenantBinding.storeText());
                }
            } else if (isNumericTenantBodyPath(path)) {
                mutated |= putLongIfPresent(objectNode, "merchant_id", tenantBinding.merchantNumeric());
                mutated |= putLongIfPresent(objectNode, "store_id", tenantBinding.storeNumeric());
            }
            return mutated ? objectMapper.writeValueAsBytes(objectNode) : body;
        } catch (Exception ex) {
            return body;
        }
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (REQUEST_HEADER_BLACKLIST.stream().anyMatch(item -> item.equalsIgnoreCase(name))) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }
        applyAuthContextHeaders(headers);
        return headers;
    }

    private void applyAuthContextHeaders(HttpHeaders headers) {
        putIfHasText(headers, RequestHeaders.REQUEST_ID, RequestIdHolder.get());
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null) {
            return;
        }
        putIfHasText(headers, RequestHeaders.USER_ID, authContext.getUserId());
        putIfHasText(headers, RequestHeaders.ACCOUNT_NAME, authContext.getAccountName());
        putIfHasText(headers, RequestHeaders.DISPLAY_NAME, authContext.getDisplayName());
        putIfHasText(headers, RequestHeaders.ACCOUNT_TYPE, authContext.getAccountType());
        putIfHasText(headers, RequestHeaders.MERCHANT_ID, authContext.getMerchantId());
        putIfHasText(headers, RequestHeaders.STORE_ID, authContext.getStoreId());
        putIfHasText(headers, RequestHeaders.MINI_PROGRAM_ID, authContext.getMiniProgramId());
        if (!authContext.getRoleCodes().isEmpty()) {
            headers.set(RequestHeaders.ROLE_CODES, String.join(",", authContext.getRoleCodes()));
        }
        headers.set(RequestHeaders.TOKEN_VERSION, Long.toString(authContext.getTokenVersion()));
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders headers) {
        HttpHeaders filtered = new HttpHeaders();
        if (headers == null) {
            return filtered;
        }
        headers.forEach((name, values) -> {
            if (RESPONSE_HEADER_BLACKLIST.stream().anyMatch(item -> item.equalsIgnoreCase(name))) {
                return;
            }
            filtered.put(name, values);
        });
        return filtered;
    }

    private void putIfHasText(HttpHeaders headers, String name, String value) {
        if (StringUtils.hasText(value) && isSafeHeaderValue(value)) {
            headers.set(name, value);
        }
    }

    private boolean isSafeHeaderValue(String value) {
        for (int index = 0; index < value.length(); index += 1) {
            char current = value.charAt(index);
            if (current < 0x20 || current > 0x7e) {
                return false;
            }
        }
        return true;
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (StringUtils.hasText(request.getContextPath()) && path.startsWith(request.getContextPath())) {
            return path.substring(request.getContextPath().length());
        }
        return path;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        try {
            return MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isConfigMerchantQueryPath(String path) {
        return "/api/config/platform/kv/effective".equals(path)
                || path.startsWith("/api/config/merchant/feature-switches")
                || path.startsWith("/api/config/publish-records");
    }

    private boolean isConfigMerchantBodyPath(String path) {
        return "/api/config/platform/kv/merchant-overrides".equals(path)
                || "/api/config/publish-records".equals(path)
                || CONFIG_SWITCH_CHANGE_PATH.matcher(path).matches();
    }

    private boolean isNumericTenantQueryPath(String path) {
        return path.startsWith("/api/report/")
                || path.startsWith("/api/product/")
                || path.startsWith("/api/order/")
                || path.startsWith("/api/release/");
    }

    private boolean isNumericTenantBodyPath(String path) {
        return "/api/product/categories/init".equals(path)
                || "/api/product/categories".equals(path)
                || PRODUCT_CATEGORY_ID_PATH.matcher(path).matches()
                || "/api/product/spu".equals(path)
                || PRODUCT_SPU_ID_PATH.matcher(path).matches()
                || "/api/product/stock/adjust".equals(path)
                || ORDER_ACTION_PATH.matcher(path).matches()
                || ORDER_AFTER_SALE_ACTION_PATH.matcher(path).matches()
                || "/api/release/tasks".equals(path);
    }

    private boolean putTextIfPresent(ObjectNode node, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        node.put(fieldName, value);
        return true;
    }

    private boolean putLongIfPresent(ObjectNode node, String fieldName, Long value) {
        if (value == null) {
            return false;
        }
        node.put(fieldName, value);
        return true;
    }

    private ResponseEntity<byte[]> gatewayUnavailable() {
        ApiResponse<Void> payload = ApiResponses.failure(CommonErrorCode.SYSTEM_ERROR, "Gateway downstream service unavailable");
        try {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Gateway downstream service unavailable".getBytes(StandardCharsets.UTF_8));
        }
    }

    private record DownstreamRequest(URI targetUri, byte[] body) {
    }

    private record TenantBinding(String merchantText, String storeText, Long merchantNumeric, Long storeNumeric) {

        static TenantBinding from(AuthContext authContext) {
            if (authContext == null) {
                return null;
            }
            return new TenantBinding(
                    emptyToNull(authContext.getMerchantId()),
                    emptyToNull(authContext.getStoreId()),
                    parseNumericTail(authContext.getMerchantId()),
                    parseNumericTail(authContext.getStoreId()));
        }

        String merchantNumericText() {
            return merchantNumeric == null ? null : Long.toString(merchantNumeric);
        }

        String storeNumericText() {
            return storeNumeric == null ? null : Long.toString(storeNumeric);
        }

        private static String emptyToNull(String value) {
            return StringUtils.hasText(value) ? value.trim() : null;
        }

        private static Long parseNumericTail(String raw) {
            if (!StringUtils.hasText(raw)) {
                return null;
            }
            StringBuilder digits = new StringBuilder();
            for (int index = raw.length() - 1; index >= 0; index--) {
                char current = raw.charAt(index);
                if (Character.isDigit(current)) {
                    digits.insert(0, current);
                    continue;
                }
                if (digits.length() > 0) {
                    break;
                }
            }
            if (digits.length() == 0) {
                return null;
            }
            try {
                return Long.parseLong(digits.toString());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
