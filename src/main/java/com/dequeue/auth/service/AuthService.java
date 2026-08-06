package com.dequeue.auth.service;

import com.dequeue.auth.dto.AuthResponse;
import com.dequeue.auth.dto.LoginRequest;
import com.dequeue.auth.dto.RefreshTokenRequest;
import com.dequeue.auth.dto.RegisterRequest;
import com.dequeue.auth.dto.StaffSummary;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    StaffSummary getCurrentUser(String userId);
    void logout(String refreshToken);
}
