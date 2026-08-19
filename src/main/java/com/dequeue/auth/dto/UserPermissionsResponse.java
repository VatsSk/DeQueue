package com.dequeue.auth.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserPermissionsResponse {
    private String userId;
    private List<String> roles;
    private List<String> permissions;
    private List<OrderStatus> orderVisibilityStatuses;
}