package com.dequeue.common.security;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacPermissionRepository;
import com.dequeue.rbac.repository.RbacRoleRepository;
import com.dequeue.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MongoTemplate mongoTemplate;
    private final RbacRoleRepository rbacRoleRepository;
    private final RbacPermissionRepository rbacPermissionRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Query query = new Query(Criteria.where("email").is(email));
        Staff staff = mongoTemplate.findOne(query, Staff.class);
        if (staff == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return buildPrincipal(staff);
    }

    public UserDetails loadUserById(String id) {
        Staff staff = mongoTemplate.findById(id, Staff.class);
        if (staff == null) {
            throw new UsernameNotFoundException("User not found with id: " + id);
        }
        return buildPrincipal(staff);
    }

    /**
     * Resolves the staff's effective permissions and order visibility by:
     * 1. Fetching all RbacRole documents referenced in staff.roleIds
     * 2. Collecting all permissionIds from those roles
     * 3. Fetching the RbacPermission documents and building permission keys
     * 4. Taking the union of orderVisibility statuses across all roles
     */
    private UserPrincipal buildPrincipal(Staff staff) {
        List<String> effectivePermissions = new ArrayList<>();
        List<OrderStatus> orderVisibilityStatuses = new ArrayList<>();

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

            java.util.Set<String> permissionKeys = new java.util.LinkedHashSet<>();
            java.util.Set<OrderStatus> visibilityStatuses = new java.util.LinkedHashSet<>();

            for (RbacRole role : roles) {
                if (role.getPermissions() != null) {
                    permissionKeys.addAll(role.getPermissions());
                }
                if (role.getOrderVisibility() != null && role.getOrderVisibility().getStatuses() != null) {
                    visibilityStatuses.addAll(role.getOrderVisibility().getStatuses());
                }
            }

            effectivePermissions = new ArrayList<>(permissionKeys);
            orderVisibilityStatuses = new ArrayList<>(visibilityStatuses);
        }

        // Platform admins see all order statuses
        if (staff.isPlatformAdmin()) {
            orderVisibilityStatuses = List.of(OrderStatus.values());
        }

        return UserPrincipal.create(staff, effectivePermissions, orderVisibilityStatuses);
    }
}
