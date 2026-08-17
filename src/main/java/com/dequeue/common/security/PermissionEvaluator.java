package com.dequeue.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component("customPermissionEvaluator")
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        // Platform admins have all permissions
        if (user.isPlatformAdmin()) {
            return true;
        }

        List<String> effectivePermissions = user.getEffectivePermissions();
        return effectivePermissions != null && effectivePermissions.contains(permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }
}
