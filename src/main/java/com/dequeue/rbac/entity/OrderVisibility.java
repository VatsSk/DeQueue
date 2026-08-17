package com.dequeue.rbac.entity;

import com.dequeue.order.entity.OrderStatus;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVisibility {

    /**
     * The order statuses this role is allowed to see.
     * e.g. Kitchen Staff: [ACCEPTED, PREPARING, READY]
     * e.g. Counter Staff: [READY]
     * e.g. Vendor Admin: all statuses
     */
    @Builder.Default
    private List<OrderStatus> statuses = new ArrayList<>();
}
