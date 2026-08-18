package com.dequeue.staff.entity;

import com.dequeue.order.entity.OrderStatus;
import java.util.List;
import java.util.ArrayList;

public enum Role {
    ROLE_VENDOR_ADMIN(
        List.of("menu.view", "menu.edit", "staff.view", "staff.edit", "order.view", "order.accept", "order.prepare", "order.ready", "order.complete", "order.cancel", "report.view"),
        List.of(OrderStatus.values())
    ),
    ROLE_VENDOR_MANAGER(
        List.of("menu.view", "menu.edit", "staff.view", "staff.edit", "order.view", "order.accept", "order.prepare", "order.ready", "order.complete", "order.cancel", "report.view"),
        List.of(OrderStatus.values())
    ),
    ROLE_VENDOR_KITCHEN(
        List.of("order.view", "order.accept", "order.prepare", "order.ready"),
        List.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY)
    ),
    ROLE_VENDOR_COUNTER(
        List.of("order.view", "order.complete", "order.cancel"),
        List.of(OrderStatus.READY)
    );

    private final List<String> permissions;
    private final List<OrderStatus> visibilityStatuses;

    Role(List<String> permissions, List<OrderStatus> visibilityStatuses) {
        this.permissions = permissions;
        this.visibilityStatuses = visibilityStatuses;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<OrderStatus> getVisibilityStatuses() {
        return visibilityStatuses;
    }

    /**
     * Resolves permissions and order visibility statuses for a list of role names.
     */
    public static ResolvedDetails resolve(List<String> roleNames) {
        List<String> permissions = new ArrayList<>();
        List<OrderStatus> statuses = new ArrayList<>();
        if (roleNames != null) {
            for (String roleName : roleNames) {
                try {
                    Role r = Role.valueOf(roleName.toUpperCase());
                    permissions.addAll(r.getPermissions());
                    for (OrderStatus s : r.getVisibilityStatuses()) {
                        if (!statuses.contains(s)) {
                            statuses.add(s);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore unrecognized roles
                }
            }
        }
        return new ResolvedDetails(permissions, statuses);
    }

    public record ResolvedDetails(List<String> permissions, List<OrderStatus> visibilityStatuses) {}
}