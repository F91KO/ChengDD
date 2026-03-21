package com.cdd.api.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TokenResponse(
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("account_type")
        String accountType,
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("access_token_expires_at")
        Instant accessTokenExpiresAt,
        @JsonProperty("refresh_token_expires_at")
        Instant refreshTokenExpiresAt) {
}
