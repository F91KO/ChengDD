package com.cdd.api.decoration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record RollbackDecorationRequest(
        @JsonProperty("merchant_id")
        @NotNull(message = "商家ID不能为空")
        Long merchantId,
        @JsonProperty("mini_program_id")
        @NotNull(message = "小程序ID不能为空")
        Long miniProgramId) {
}
