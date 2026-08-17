package com.dequeue.rbac.dto;

import com.dequeue.rbac.entity.OrderVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRoleRequest {

    @NotBlank
    private String name;

    private String description;

    private List<String> permissionIds;

    private OrderVisibility orderVisibility;

    private Boolean active;
}
