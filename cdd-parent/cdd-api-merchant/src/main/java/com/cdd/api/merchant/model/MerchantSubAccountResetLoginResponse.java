package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantSubAccountResetLoginResponse(
        @JsonProperty("account_id")
        String accountId,
        @JsonProperty("temporary_password")
        String temporaryPassword) {
}
