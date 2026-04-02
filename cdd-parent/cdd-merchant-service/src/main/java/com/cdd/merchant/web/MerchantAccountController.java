package com.cdd.merchant.web;

import com.cdd.api.merchant.model.MerchantSubAccountResetLoginResponse;
import com.cdd.api.merchant.model.MerchantSubAccountResponse;
import com.cdd.api.merchant.model.MerchantSubAccountUpsertRequest;
import com.cdd.common.core.page.PageQuery;
import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireRoles;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.common.web.PageResponse;
import com.cdd.merchant.service.MerchantAccountApplicationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/merchant/accounts/sub-accounts")
@RequireAccountTypes({"merchant"})
@RequireRoles(anyOf = {"merchant_owner"})
@RequireScope(requireMerchant = true)
public class MerchantAccountController {

    private final MerchantAccountApplicationService merchantAccountApplicationService;

    public MerchantAccountController(MerchantAccountApplicationService merchantAccountApplicationService) {
        this.merchantAccountApplicationService = merchantAccountApplicationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MerchantSubAccountResponse>> listSubAccounts(
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", required = false, defaultValue = "20") Integer pageSize) {
        return ApiResponses.success(merchantAccountApplicationService.pageSubAccounts(new PageQuery(page, pageSize)));
    }

    @PostMapping
    public ApiResponse<MerchantSubAccountResponse> createSubAccount(
            @Valid @RequestBody MerchantSubAccountUpsertRequest request) {
        return ApiResponses.success(merchantAccountApplicationService.createSubAccount(request));
    }

    @PutMapping("/{account_id}")
    public ApiResponse<MerchantSubAccountResponse> updateSubAccount(
            @PathVariable("account_id") Long accountId,
            @Valid @RequestBody MerchantSubAccountUpsertRequest request) {
        return ApiResponses.success(merchantAccountApplicationService.updateSubAccount(accountId, request));
    }

    @PostMapping("/{account_id}/disable")
    public ApiResponse<MerchantSubAccountResponse> disableSubAccount(@PathVariable("account_id") Long accountId) {
        return ApiResponses.success(merchantAccountApplicationService.disableSubAccount(accountId));
    }

    @PostMapping("/{account_id}/reset-login")
    public ApiResponse<MerchantSubAccountResetLoginResponse> resetLogin(@PathVariable("account_id") Long accountId) {
        return ApiResponses.success(merchantAccountApplicationService.resetLogin(accountId));
    }
}
