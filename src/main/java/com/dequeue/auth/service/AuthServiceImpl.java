package com.dequeue.auth.service;

import com.dequeue.auth.dto.*;
import com.dequeue.auth.entity.RefreshToken;
import com.dequeue.auth.repository.RefreshTokenRepository;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.exception.UnauthorizedException;
import com.dequeue.common.security.JwtTokenProvider;
import com.dequeue.common.security.UserPrincipal;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacRoleRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final StaffRepository staffRepository;
    private final VendorRepository vendorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RbacRoleRepository rbacRoleRepository;

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

        // Resolve effective permissions & order visibility from roles
        ResolvedPermissions resolved = resolvePermissions(staff);

        // Build UserPrincipal so we can generate the token consistently
        UserPrincipal principal = UserPrincipal.create(staff, resolved.roleNames(), resolved.permissionKeys(), resolved.visibilityStatuses());
        String accessToken = jwtTokenProvider.generateToken(principal);
        RefreshToken refreshToken = createRefreshToken(staff);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor, resolved))
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
        vendor.setEmail(request.getEmail());
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
        staff.setDepartmentIds(new ArrayList<>());
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setVendorId(vendor.getId());

        // Assign the global ROLE_VENDOR_ADMIN role by its ID
        rbacRoleRepository.findByName("ROLE_VENDOR_ADMIN").ifPresentOrElse(
                adminRole -> staff.setRoles(List.of(adminRole.getId())),
                () -> staff.setRoles(new ArrayList<>())
        );

        staffRepository.save(staff);

        ResolvedPermissions resolved = resolvePermissions(staff);
        UserPrincipal principal = UserPrincipal.create(staff, resolved.roleNames(), resolved.permissionKeys(), resolved.visibilityStatuses());
        String accessToken = jwtTokenProvider.generateToken(principal);
        RefreshToken refreshToken = createRefreshToken(staff);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor, resolved))
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

        ResolvedPermissions resolved = resolvePermissions(staff);
        UserPrincipal principal = UserPrincipal.create(staff, resolved.roleNames(), resolved.permissionKeys(), resolved.visibilityStatuses());
        String accessToken = jwtTokenProvider.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(mapToSummary(staff, vendor, resolved))
                .build();
    }

    @Override
    public StaffSummary getCurrentUser(String userId) {
        Staff staff = staffRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Vendor vendor = vendorRepository.findById(staff.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        ResolvedPermissions resolved = resolvePermissions(staff);
        return mapToSummary(staff, vendor, resolved);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    // ────────────────────────── private helpers ──────────────────────────

    private ResolvedPermissions resolvePermissions(Staff staff) {
        List<String> permissionKeys = new ArrayList<>();
        List<OrderStatus> visibilityStatuses = new ArrayList<>();

        if (staff.getRoles() != null && !staff.getRoles().isEmpty()) {
            List<String> roleNames = new ArrayList<>();
            List<String> roleIds = new ArrayList<>();
            for (String r : staff.getRoles()) {
                if (r != null && r.length() == 24 && r.matches("^[0-9a-fA-F]{24}$")) {
                    roleIds.add(r);
                } else if (r != null) {
                    roleNames.add(r);
                }
            }

            List<RbacRole> roles = new ArrayList<>();
            if (!roleIds.isEmpty()) {
                rbacRoleRepository.findAllById(roleIds).forEach(roles::add);
            }
            // Legacy fallback: staff.roles stored as name strings
            if (!roleNames.isEmpty()) {
                rbacRoleRepository.findByNameIn(roleNames).forEach(roles::add);
            }

            java.util.Set<String> actualRoleNames = new java.util.LinkedHashSet<>();
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            java.util.Set<OrderStatus> statuses = new java.util.LinkedHashSet<>();

            for (RbacRole role : roles) {
                if (role.getName() != null) {
                    actualRoleNames.add(role.getName());
                }
                if (role.getPermissions() != null) {
                    keys.addAll(role.getPermissions());
                }
                if (role.getOrderVisibility() != null && role.getOrderVisibility().getStatuses() != null) {
                    statuses.addAll(role.getOrderVisibility().getStatuses());
                }
            }

            permissionKeys = new ArrayList<>(keys);
            visibilityStatuses = new ArrayList<>(statuses);
            List<String> finalRoleNames = new ArrayList<>(actualRoleNames);
            if (staff.isPlatformAdmin()) {
                visibilityStatuses = Arrays.asList(OrderStatus.values());
            }
            return new ResolvedPermissions(permissionKeys, visibilityStatuses, finalRoleNames);
        }

        if (staff.isPlatformAdmin()) {
            visibilityStatuses = Arrays.asList(OrderStatus.values());
        }

        return new ResolvedPermissions(permissionKeys, visibilityStatuses, new ArrayList<>());
    }

    private StaffSummary mapToSummary(Staff staff, Vendor vendor, ResolvedPermissions resolved) {
        return StaffSummary.builder()
                .id(staff.getId())
                .name(staff.getName())
                .email(staff.getEmail())
                .roleIds(staff.getRoles())
                .roleNames(resolved.roleNames())
                .effectivePermissions(resolved.permissionKeys())
                .orderVisibilityStatuses(resolved.visibilityStatuses())
                .departmentIds(staff.getDepartmentIds())
                .vendorId(staff.getVendorId())
                .vendorCode(vendor.getVendorCode())
                .shopName(vendor.getShopName())
                .platformAdmin(staff.isPlatformAdmin())
                .build();
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

    /** Simple value holder for resolved permission data */
    private record ResolvedPermissions(List<String> permissionKeys, List<OrderStatus> visibilityStatuses, List<String> roleNames) {}
}
