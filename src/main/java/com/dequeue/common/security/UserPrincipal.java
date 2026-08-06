package com.dequeue.common.security;

import com.dequeue.staff.entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
    private String role;
    private String department;
    private List<String> permissions;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(Staff staff) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        List<String> permissionNames = new java.util.ArrayList<>();

        if (staff.getPermissions() != null) {
            authorities = staff.getPermissions().stream()
                    .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission.name()))
                    .collect(Collectors.toList());
            permissionNames = staff.getPermissions().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
        
        authorities.add(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()));

        return UserPrincipal.builder()
                .id(staff.getId())
                .vendorId(staff.getVendorId())
                .name(staff.getName())
                .email(staff.getEmail())
                .password(staff.getPassword())
                .role(staff.getRole().name())
                .department(staff.getDepartmentId())
                .permissions(permissionNames)
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
