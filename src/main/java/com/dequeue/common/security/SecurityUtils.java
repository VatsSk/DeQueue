package com.dequeue.common.security;

import com.dequeue.order.entity.OrderStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

public class SecurityUtils {

    private SecurityUtils() {}

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

    public static String getCurrentUserName() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getName() : null;
    }

    public static UserPrincipal getCurrentUserPrincipal() {
        return getCurrentUser();
    }

    /**
     * Returns the effective permission keys for the current user.
     * e.g. ["order.view", "order.accept", "menu.view"]
     */
    public static List<String> getCurrentEffectivePermissions() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getEffectivePermissions() : new ArrayList<>();
    }

    /**
     * Returns the order statuses the current user is allowed to see,
     * based on the union of orderVisibility.statuses from their assigned roles.
     * Returns an empty list if not set (callers should treat empty as "see all").
     */
    public static List<OrderStatus> getCurrentOrderVisibilityStatuses() {
        UserPrincipal user = getCurrentUser();
        if (user == null) return new ArrayList<>();
        return user.getOrderVisibilityStatuses();
    }

    /**
     * Checks whether the current user has a specific permission key.
     * e.g. hasPermission("order.accept")
     */
    public static boolean hasPermission(String permissionKey) {
        List<String> permissions = getCurrentEffectivePermissions();
        return permissions.contains(permissionKey);
    }

    /**
     * Returns true if the current user is a DeQueue Platform Admin.
     */
    public static boolean isPlatformAdmin() {
        UserPrincipal user = getCurrentUser();
        return user != null && user.isPlatformAdmin();
    }
}
