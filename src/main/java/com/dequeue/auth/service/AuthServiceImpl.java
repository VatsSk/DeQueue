package com.dequeue.auth.service;

import com.dequeue.auth.dto.*;
import com.dequeue.auth.entity.RefreshToken;
import com.dequeue.auth.repository.RefreshTokenRepository;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.exception.UnauthorizedException;
import com.dequeue.common.security.JwtTokenProvider;
import com.dequeue.staff.entity.Role;
import com.dequeue.staff.entity.Staff;
import com.dequeue.staff.entity.StaffStatus;
import com.dequeue.staff.repository.StaffRepository;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final StaffRepository staffRepository;
    private final VendorRepository vendorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        Staff staff = staffRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (staff.getStatus() != StaffStatus.ACTIVE) {
            throw new UnauthorizedException("Staff account is not active");
        }

        Vendor vendor = vendorRepository.findById(staff.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        if (!vendor.isActive()) {
            throw new UnauthorizedException("Vendor account is not active");
        }

        String accessToken = jwtTokenProvider.generateToken(staff);
        RefreshToken refreshToken = createRefreshToken(staff);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String vendorCode = request.getShopName().toLowerCase().replaceAll("[^a-z0-9]", "-") 
                + "-" + UUID.randomUUID().toString().substring(0, 4);

        Vendor vendor = new Vendor();
        vendor.setShopName(request.getShopName());
        vendor.setOwnerName(request.getOwnerName());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(com.dequeue.vendor.entity.Address.builder().street(request.getAddress()).build());
        vendor.setVendorCode(vendorCode);
        vendor.setShopStatus(ShopStatus.CLOSED);
        vendor.setActive(true);
        vendor = vendorRepository.save(vendor);

        Staff staff = new Staff();
        staff.setName(request.getOwnerName());
        staff.setEmail(request.getEmail());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setPhone(request.getPhone());
        staff.setRole(Role.ADMIN);
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setVendorId(vendor.getId());
        staff.setPermissions(new ArrayList<>());
        staff = staffRepository.save(staff);

        String accessToken = jwtTokenProvider.generateToken(staff);
        RefreshToken refreshToken = createRefreshToken(staff);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor))
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        Staff staff = staffRepository.findById(refreshToken.getStaffId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Vendor vendor = vendorRepository.findById(staff.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        String accessToken = jwtTokenProvider.generateToken(staff);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor))
                .build();
    }

    @Override
    public StaffSummary getCurrentUser(String userId) {
        Staff staff = staffRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Vendor vendor = vendorRepository.findById(staff.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return mapToSummary(staff, vendor);
    }
    
    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    private RefreshToken createRefreshToken(Staff staff) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .staffId(staff.getId())
                .vendorId(staff.getVendorId())
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationTime()))
                .createdAt(Instant.now())
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private StaffSummary mapToSummary(Staff staff, Vendor vendor) {
        return StaffSummary.builder()
                .id(staff.getId())
                .name(staff.getName())
                .email(staff.getEmail())
                .role(staff.getRole())
                .department(staff.getDepartmentId())
                .permissions(staff.getPermissions())
                .vendorId(staff.getVendorId())
                .vendorCode(vendor.getVendorCode())
                .shopName(vendor.getShopName())
                .build();
    }
}
