package com.dequeue.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    public static String getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
    
    public static String getCurrentVendorId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getVendorId() : null;
    }

    public static UserPrincipal getCurrentUserPrincipal() {
        return getCurrentUser();
    }
}
