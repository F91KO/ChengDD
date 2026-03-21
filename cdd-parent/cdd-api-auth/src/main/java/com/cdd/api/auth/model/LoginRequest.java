package com.cdd.api.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonProperty("account_name")
        @NotBlank(message = "账号不能为空")
        String accountName,
        @NotBlank(message = "密码不能为空")
        String password) {
}
