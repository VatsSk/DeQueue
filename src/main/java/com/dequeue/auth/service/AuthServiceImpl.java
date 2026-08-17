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
import com.dequeue.rbac.entity.OrderVisibility;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacPermissionRepository;
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
    private final RbacPermissionRepository rbacPermissionRepository;

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
        UserPrincipal principal = UserPrincipal.create(staff, resolved.permissionKeys, resolved.visibilityStatuses);
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

        // Create a default "Vendor Admin" role with all active permissions for this new vendor
        List<RbacPermission> allPermissions = rbacPermissionRepository.findByActiveTrue();
        List<String> allPermissionIds = allPermissions.stream()
                .map(RbacPermission::getId)
                .collect(Collectors.toList());

        OrderVisibility allStatuses = OrderVisibility.builder()
                .statuses(Arrays.asList(OrderStatus.values()))
                .build();

        RbacRole adminRole = RbacRole.builder()
                .vendorId(vendor.getId())
                .name("Vendor Admin")
                .description("Full access role for vendor administrator")
                .permissionIds(allPermissionIds)
                .orderVisibility(allStatuses)
                .active(true)
                .build();
        adminRole = rbacRoleRepository.save(adminRole);

        Staff staff = new Staff();
        staff.setName(request.getOwnerName());
        staff.setEmail(request.getEmail());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setPhone(request.getPhone());
        staff.setRoleIds(List.of(adminRole.getId()));
        staff.setDepartmentIds(new ArrayList<>());
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setVendorId(vendor.getId());
        staff = staffRepository.save(staff);

        ResolvedPermissions resolved = resolvePermissions(staff);
        UserPrincipal principal = UserPrincipal.create(staff, resolved.permissionKeys, resolved.visibilityStatuses);
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
        UserPrincipal principal = UserPrincipal.create(staff, resolved.permissionKeys, resolved.visibilityStatuses);
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

    /**
     * Resolves effective permissions and order visibility by fetching
     * the staff's assigned RbacRole documents and their RbacPermission documents.
     */
    private ResolvedPermissions resolvePermissions(Staff staff) {
        List<String> permissionKeys = new ArrayList<>();
        List<OrderStatus> visibilityStatuses = new ArrayList<>();

        if (staff.getRoleIds() != null && !staff.getRoleIds().isEmpty()) {
            List<RbacRole> roles = staff.isPlatformAdmin()
                    ? rbacRoleRepository.findByIdIn(staff.getRoleIds())
                    : rbacRoleRepository.findByIdInAndVendorId(staff.getRoleIds(), staff.getVendorId());

            Set<String> permissionIds = new LinkedHashSet<>();
            Set<OrderStatus> statuses = new LinkedHashSet<>();

            for (RbacRole role : roles) {
                if (role.getPermissionIds() != null) permissionIds.addAll(role.getPermissionIds());
                if (role.getOrderVisibility() != null && role.getOrderVisibility().getStatuses() != null) {
                    statuses.addAll(role.getOrderVisibility().getStatuses());
                }
            }

            if (!permissionIds.isEmpty()) {
                permissionKeys = rbacPermissionRepository.findByIdIn(new ArrayList<>(permissionIds)).stream()
                        .filter(RbacPermission::isActive)
                        .map(RbacPermission::getPermissionKey)
                        .collect(Collectors.toList());
            }
            visibilityStatuses = new ArrayList<>(statuses);
        }

        if (staff.isPlatformAdmin()) {
            visibilityStatuses = Arrays.asList(OrderStatus.values());
        }

        return new ResolvedPermissions(permissionKeys, visibilityStatuses);
    }

    private StaffSummary mapToSummary(Staff staff, Vendor vendor, ResolvedPermissions resolved) {
        // Resolve role names
        List<String> roleNames = new ArrayList<>();
        if (staff.getRoleIds() != null && !staff.getRoleIds().isEmpty()) {
            roleNames = rbacRoleRepository.findByIdIn(staff.getRoleIds()).stream()
                    .map(RbacRole::getName)
                    .collect(Collectors.toList());
        }

        return StaffSummary.builder()
                .id(staff.getId())
                .name(staff.getName())
                .email(staff.getEmail())
                .roleIds(staff.getRoleIds())
                .roleNames(roleNames)
                .effectivePermissions(resolved.permissionKeys)
                .orderVisibilityStatuses(resolved.visibilityStatuses)
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
    private record ResolvedPermissions(List<String> permissionKeys, List<OrderStatus> visibilityStatuses) {}
}
