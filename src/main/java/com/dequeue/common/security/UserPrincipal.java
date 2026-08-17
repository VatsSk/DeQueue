package com.dequeue.common.security;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.staff.entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private String id;
    private String vendorId;
    private String name;
    private String email;
    private String password;

    /** IDs of the RbacRole documents assigned to this user */
    @Builder.Default
    private List<String> roleIds = new ArrayList<>();

    /** Department IDs the user belongs to */
    @Builder.Default
    private List<String> departmentIds = new ArrayList<>();

    /**
     * Effective permissions derived from all assigned roles.
     * Formatted as "resource.action", e.g. "order.accept", "menu.view".
     * This is returned to Android clients for dynamic UI rendering.
     */
    @Builder.Default
    private List<String> effectivePermissions = new ArrayList<>();

    /**
     * Union of orderVisibility.statuses from all assigned roles.
     * Used to filter which orders this user can see.
     */
    @Builder.Default
    private List<OrderStatus> orderVisibilityStatuses = new ArrayList<>();

    /** True for DeQueue Platform Admin users */
    private boolean platformAdmin;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates a UserPrincipal with fully-resolved effective permissions and
     * order visibility. Called by CustomUserDetailsService after fetching roles.
     */
    public static UserPrincipal create(Staff staff,
                                       List<String> effectivePermissions,
                                       List<OrderStatus> orderVisibilityStatuses) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Grant PERM_ authority for each effective permission key
        if (effectivePermissions != null) {
            effectivePermissions.stream()
                    .map(p -> new SimpleGrantedAuthority("PERM_" + p.toUpperCase().replace(".", "_")))
                    .forEach(authorities::add);
        }

        // Grant ROLE_PLATFORM_ADMIN for platform admins
        if (staff.isPlatformAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }

        return UserPrincipal.builder()
                .id(staff.getId())
                .vendorId(staff.getVendorId())
                .name(staff.getName())
                .email(staff.getEmail())
                .password(staff.getPassword())
                .roleIds(staff.getRoleIds() != null ? staff.getRoleIds() : new ArrayList<>())
                .departmentIds(staff.getDepartmentIds() != null ? staff.getDepartmentIds() : new ArrayList<>())
                .effectivePermissions(effectivePermissions != null ? effectivePermissions : new ArrayList<>())
                .orderVisibilityStatuses(orderVisibilityStatuses != null ? orderVisibilityStatuses : new ArrayList<>())
                .platformAdmin(staff.isPlatformAdmin())
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
