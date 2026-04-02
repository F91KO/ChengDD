package com.cdd.common.security.authentication;

import static org.junit.jupiter.api.Assertions.*;

import com.cdd.common.security.config.CddSecurityProperties;
import com.cdd.common.security.context.AuthContext;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class NimbusJwtTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-at-least-32-bytes-long!!";

    private NimbusJwtTokenService createService(Duration accessTtl) {
        CddSecurityProperties props = new CddSecurityProperties();
        props.getJwt().setSecret(SECRET);
        props.getJwt().setIssuer("test-issuer");
        props.getJwt().setAccessTokenTtl(accessTtl);
        props.getJwt().setRefreshTokenTtl(Duration.ofDays(1));
        return new NimbusJwtTokenService(props);
    }

    private AuthContext sampleContext() {
        return new AuthContext(null, "user-1", "admin", "管理员", "MERCHANT",
                "M001", "S001", "MP001", List.of("ROLE_ADMIN"), List.of("product"), List.of("view"), 1L);
    }

    @Test
    void createAndParseAccessToken() {
        NimbusJwtTokenService service = createService(Duration.ofMinutes(30));
        String token = service.createAccessToken(sampleContext());
        JwtParsedToken parsed = service.parseAccessToken(token);

        assertEquals(JwtTokenType.ACCESS, parsed.tokenType());
        assertEquals("user-1", parsed.authContext().getUserId());
        assertEquals("admin", parsed.authContext().getAccountName());
        assertEquals("M001", parsed.authContext().getMerchantId());
        assertEquals(List.of("ROLE_ADMIN"), parsed.authContext().getRoleCodes());
        assertEquals(List.of("product"), parsed.authContext().getPermissionModules());
        assertEquals(List.of("view"), parsed.authContext().getActionPermissions());
    }

    @Test
    void createAndParseRefreshToken() {
        NimbusJwtTokenService service = createService(Duration.ofMinutes(30));
        String token = service.createRefreshToken(sampleContext());
        JwtParsedToken parsed = service.parseRefreshToken(token);

        assertEquals(JwtTokenType.REFRESH, parsed.tokenType());
        assertEquals("user-1", parsed.authContext().getUserId());
    }

    @Test
    void expiredTokenThrows() throws Exception {
        NimbusJwtTokenService service = createService(Duration.ofSeconds(1));
        String token = service.createAccessToken(sampleContext());
        Thread.sleep(1500);
        assertThrows(JwtAuthenticationException.class, () -> service.parseAccessToken(token));
    }

    @Test
    void tamperedTokenThrows() {
        NimbusJwtTokenService service = createService(Duration.ofMinutes(30));
        String token = service.createAccessToken(sampleContext());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThrows(JwtAuthenticationException.class, () -> service.parseAccessToken(tampered));
    }

    @Test
    void shortSecretThrows() {
        CddSecurityProperties props = new CddSecurityProperties();
        props.getJwt().setSecret("short");
        assertThrows(IllegalArgumentException.class, () -> new NimbusJwtTokenService(props));
    }

    @Test
    void accessTokenCannotBeParsedAsRefresh() {
        NimbusJwtTokenService service = createService(Duration.ofMinutes(30));
        String token = service.createAccessToken(sampleContext());
        assertThrows(JwtAuthenticationException.class, () -> service.parseRefreshToken(token));
    }

    @Test
    void emptyTokenThrows() {
        NimbusJwtTokenService service = createService(Duration.ofMinutes(30));
        assertThrows(JwtAuthenticationException.class, () -> service.parseAccessToken(""));
        assertThrows(JwtAuthenticationException.class, () -> service.parseAccessToken(null));
    }
}
