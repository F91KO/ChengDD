package com.cdd.common.security.authentication;

import com.cdd.common.security.config.CddSecurityProperties;
import com.cdd.common.security.context.AuthContext;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

public class NimbusJwtTokenService implements JwtTokenService {

    private final CddSecurityProperties securityProperties;
    private final byte[] secretBytes;

    public NimbusJwtTokenService(CddSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.secretBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret 长度至少需要 32 字节");
        }
    }

    @Override
    public String createAccessToken(AuthContext authContext) {
        return createToken(authContext, JwtTokenType.ACCESS, securityProperties.getJwt().getAccessTokenTtl().toSeconds());
    }

    @Override
    public String createRefreshToken(AuthContext authContext) {
        return createToken(authContext, JwtTokenType.REFRESH, securityProperties.getJwt().getRefreshTokenTtl().toSeconds());
    }

    @Override
    public JwtParsedToken parseAccessToken(String token) {
        return parseToken(token, JwtTokenType.ACCESS);
    }

    @Override
    public JwtParsedToken parseRefreshToken(String token) {
        return parseToken(token, JwtTokenType.REFRESH);
    }

    private String createToken(AuthContext authContext, JwtTokenType tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issuer(securityProperties.getJwt().getIssuer())
                .subject(authContext.getUserId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .claim(JwtClaims.TOKEN_TYPE, tokenType.value())
                .claim(JwtClaims.ACCOUNT_NAME, authContext.getAccountName())
                .claim(JwtClaims.DISPLAY_NAME, authContext.getDisplayName())
                .claim(JwtClaims.ACCOUNT_TYPE, authContext.getAccountType())
                .claim(JwtClaims.MERCHANT_ID, authContext.getMerchantId())
                .claim(JwtClaims.STORE_ID, authContext.getStoreId())
                .claim(JwtClaims.MINI_PROGRAM_ID, authContext.getMiniProgramId())
                .claim(JwtClaims.ROLE_CODES, authContext.getRoleCodes())
                .claim(JwtClaims.PERMISSION_MODULES, authContext.getPermissionModules())
                .claim(JwtClaims.ACTION_PERMISSIONS, authContext.getActionPermissions())
                .claim(JwtClaims.TOKEN_VERSION, authContext.getTokenVersion())
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        try {
            signedJWT.sign(new MACSigner(secretBytes));
            return signedJWT.serialize();
        } catch (JOSEException ex) {
            throw new JwtAuthenticationException("令牌签发失败");
        }
    }

    private JwtParsedToken parseToken(String token, JwtTokenType expectedTokenType) {
        if (!StringUtils.hasText(token)) {
            throw new JwtAuthenticationException("认证令牌不能为空");
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(new MACVerifier(secretBytes))) {
                throw new JwtAuthenticationException("令牌签名无效");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (!securityProperties.getJwt().getIssuer().equals(claimsSet.getIssuer())) {
                throw new JwtAuthenticationException("令牌签发方无效");
            }
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.toInstant().isBefore(Instant.now())) {
                throw new JwtAuthenticationException("令牌已过期");
            }

            JwtTokenType actualTokenType = JwtTokenType.from(claimsSet.getStringClaim(JwtClaims.TOKEN_TYPE));
            if (actualTokenType != expectedTokenType) {
                throw new JwtAuthenticationException(expectedTokenType == JwtTokenType.ACCESS ? "访问令牌无效" : "刷新令牌无效");
            }

            AuthContext authContext = new AuthContext(
                    null,
                    claimsSet.getSubject(),
                    claimsSet.getStringClaim(JwtClaims.ACCOUNT_NAME),
                    claimsSet.getStringClaim(JwtClaims.DISPLAY_NAME),
                    claimsSet.getStringClaim(JwtClaims.ACCOUNT_TYPE),
                    claimsSet.getStringClaim(JwtClaims.MERCHANT_ID),
                    claimsSet.getStringClaim(JwtClaims.STORE_ID),
                    claimsSet.getStringClaim(JwtClaims.MINI_PROGRAM_ID),
                    readRoleCodes(claimsSet),
                    readPermissionModules(claimsSet),
                    readActionPermissions(claimsSet),
                    readTokenVersion(claimsSet));

            return new JwtParsedToken(
                    claimsSet.getJWTID(),
                    actualTokenType,
                    claimsSet.getIssueTime() == null ? Instant.now() : claimsSet.getIssueTime().toInstant(),
                    expirationTime.toInstant(),
                    authContext);
        } catch (ParseException | JOSEException ex) {
            throw new JwtAuthenticationException("令牌解析失败");
        }
    }

    private List<String> readRoleCodes(JWTClaimsSet claimsSet) throws ParseException {
        List<String> roleCodes = claimsSet.getStringListClaim(JwtClaims.ROLE_CODES);
        return roleCodes == null ? List.of() : List.copyOf(roleCodes);
    }

    private long readTokenVersion(JWTClaimsSet claimsSet) throws ParseException {
        Number tokenVersion = claimsSet.getLongClaim(JwtClaims.TOKEN_VERSION);
        return tokenVersion == null ? 0L : tokenVersion.longValue();
    }

    private List<String> readPermissionModules(JWTClaimsSet claimsSet) throws ParseException {
        List<String> permissionModules = claimsSet.getStringListClaim(JwtClaims.PERMISSION_MODULES);
        return permissionModules == null ? List.of() : List.copyOf(permissionModules);
    }

    private List<String> readActionPermissions(JWTClaimsSet claimsSet) throws ParseException {
        List<String> actionPermissions = claimsSet.getStringListClaim(JwtClaims.ACTION_PERMISSIONS);
        return actionPermissions == null ? List.of() : List.copyOf(actionPermissions);
    }
}
