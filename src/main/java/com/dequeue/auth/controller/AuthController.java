package com.dequeue.auth.controller;

import com.dequeue.auth.dto.AuthResponse;
import com.dequeue.auth.dto.LoginRequest;
import com.dequeue.auth.dto.RefreshTokenRequest;
import com.dequeue.auth.dto.RegisterRequest;
import com.dequeue.auth.dto.StaffSummary;
import com.dequeue.auth.service.AuthService;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }
    
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<StaffSummary> getCurrentUser() {
        return ApiResponse.success(authService.getCurrentUser(SecurityUtils.getCurrentUserId()));
    }
}
