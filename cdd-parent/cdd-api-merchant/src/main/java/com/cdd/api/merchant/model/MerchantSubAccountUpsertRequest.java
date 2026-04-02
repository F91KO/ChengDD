package com.cdd.api.merchant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MerchantSubAccountUpsertRequest(
        @JsonProperty("account_name")
        @NotBlank(message = "登录账号不能为空")
        String accountName,
        @JsonProperty("display_name")
        @NotBlank(message = "显示名称不能为空")
        String displayName,
        @JsonProperty("mobile")
        @NotBlank(message = "手机号不能为空")
        String mobile,
        @JsonProperty("remark")
        String remark,
        @JsonProperty("permission_modules")
        @NotEmpty(message = "模块权限不能为空")
        List<String> permissionModules,
        @JsonProperty("action_permissions")
        @NotEmpty(message = "动作权限不能为空")
        List<String> actionPermissions,
        @JsonProperty("data_scope_type")
        @NotBlank(message = "数据范围类型不能为空")
        String dataScopeType,
        @JsonProperty("data_scope_ids")
        List<String> dataScopeIds) {
}
