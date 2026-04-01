package com.cdd.gateway.web;

import com.cdd.api.auth.model.CurrentAuthContextResponse;
import com.cdd.common.core.error.BusinessException;
import com.cdd.common.core.error.CommonErrorCode;
import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireRoles;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateway")
public class GatewayContextController {

    @GetMapping("/context")
    @RequireAccountTypes({"platform", "merchant"})
    @RequireRoles(anyOf = {"platform_admin", "merchant_owner", "merchant_admin"})
    @RequireScope
    public ApiResponse<CurrentAuthContextResponse> current() {
        AuthContext authContext = AuthContextHolder.get();
        if (authContext == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return ApiResponses.success(new CurrentAuthContextResponse(
                authContext.getUserId(),
                authContext.getAccountName(),
                authContext.getDisplayName(),
                authContext.getAccountType(),
                authContext.getMerchantId(),
                authContext.getStoreId(),
                authContext.getMiniProgramId(),
                authContext.getRoleCodes(),
                authContext.getTokenVersion()));
    }
}
