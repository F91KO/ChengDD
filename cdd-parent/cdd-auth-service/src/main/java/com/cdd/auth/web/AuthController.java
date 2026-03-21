package com.cdd.auth.web;

import com.cdd.api.auth.model.CurrentAuthContextResponse;
import com.cdd.api.auth.model.LoginRequest;
import com.cdd.api.auth.model.LogoutRequest;
import com.cdd.api.auth.model.RefreshTokenRequest;
import com.cdd.api.auth.model.TokenResponse;
import com.cdd.auth.service.AuthApplicationService;
import com.cdd.common.security.authorization.RequireAccountTypes;
import com.cdd.common.security.authorization.RequireScope;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/platform/login")
    public ApiResponse<TokenResponse> platformLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponses.success(authApplicationService.login("platform", request));
    }

    @PostMapping("/merchant/login")
    public ApiResponse<TokenResponse> merchantLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponses.success(authApplicationService.login("merchant", request));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponses.success(authApplicationService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(request);
        return ApiResponses.success(null);
    }

    @GetMapping("/me")
    @RequireAccountTypes({"platform", "merchant"})
    @RequireScope
    public ApiResponse<CurrentAuthContextResponse> current() {
        return ApiResponses.success(authApplicationService.current());
    }
}
