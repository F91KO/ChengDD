package com.cdd.api.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @JsonProperty("refresh_token")
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken) {
}
