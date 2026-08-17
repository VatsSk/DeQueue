package com.dequeue.order.service;

import com.dequeue.common.exception.WorkflowConflictException;
import com.dequeue.order.entity.OrderStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines the valid order state machine transitions and the required
 * permission key for each target status.
 *
 * Valid transitions:
 *   PENDING   → ACCEPTED, CANCELLED
 *   ACCEPTED  → PREPARING, CANCELLED
 *   PREPARING → READY, CANCELLED
 *   READY     → COMPLETED
 *   COMPLETED → (terminal)
 *   CANCELLED → (terminal)
 */
public final class OrderStateMachine {

    private OrderStateMachine() {}

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING,   EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED,  EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY,     EnumSet.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    private static final Map<OrderStatus, String> REQUIRED_PERMISSION = Map.of(
            OrderStatus.ACCEPTED,  "order.accept",
            OrderStatus.PREPARING, "order.prepare",
            OrderStatus.READY,     "order.ready",
            OrderStatus.COMPLETED, "order.complete",
            OrderStatus.CANCELLED, "order.cancel"
    );

    /**
     * Validates that transitioning from {@code current} to {@code next} is allowed.
     *
     * @throws WorkflowConflictException if the transition is invalid
     */
    public static void validate(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(next)) {
            throw new WorkflowConflictException(
                    "Cannot transition order from " + current + " to " + next +
                    ". Allowed next statuses: " + allowed);
        }
    }

    /**
     * Returns the permission key required to perform the transition to {@code targetStatus}.
     * e.g. ACCEPTED → "order.accept"
     */
    public static String requiredPermission(OrderStatus targetStatus) {
        return REQUIRED_PERMISSION.get(targetStatus);
    }

    /**
     * Returns the set of statuses from which an order can be cancelled.
     */
    public static Set<OrderStatus> cancellableStatuses() {
        return EnumSet.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PREPARING);
    }
}
