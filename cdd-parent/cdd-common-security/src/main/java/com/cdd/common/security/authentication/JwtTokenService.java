package com.cdd.common.security.authentication;

import com.cdd.common.security.context.AuthContext;

public interface JwtTokenService {

    String createAccessToken(AuthContext authContext);

    String createRefreshToken(AuthContext authContext);

    JwtParsedToken parseAccessToken(String token);

    JwtParsedToken parseRefreshToken(String token);
}
