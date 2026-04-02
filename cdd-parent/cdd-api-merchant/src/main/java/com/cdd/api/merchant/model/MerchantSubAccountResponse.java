package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MerchantSubAccountResponse(
        @JsonProperty("account_id")
        String accountId,
        @JsonProperty("account_name")
        String accountName,
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty("mobile")
        String mobile,
        @JsonProperty("remark")
        String remark,
        @JsonProperty("status")
        String status,
        @JsonProperty("role_label")
        String roleLabel,
        @JsonProperty("permission_modules")
        List<String> permissionModules,
        @JsonProperty("action_permissions")
        List<String> actionPermissions,
        @JsonProperty("data_scope_type")
        String dataScopeType,
        @JsonProperty("data_scope_ids")
        List<String> dataScopeIds) {
}
