package com.dequeue.rbac.dto;

import com.dequeue.rbac.entity.OrderVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {

    @NotBlank
    private String name;

    private String description;

    @NotEmpty(message = "A role must have at least one permission")
    private List<String> permissionIds;

    private OrderVisibility orderVisibility;
}
