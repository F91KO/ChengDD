package com.cdd.api.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CurrentAuthContextResponse(
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("account_name")
        String accountName,
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty("account_type")
        String accountType,
        @JsonProperty("merchant_id")
        String merchantId,
        @JsonProperty("store_id")
        String storeId,
        @JsonProperty("mini_program_id")
        String miniProgramId,
        @JsonProperty("role_codes")
        List<String> roleCodes,
        @JsonProperty("token_version")
        long tokenVersion) {
}
