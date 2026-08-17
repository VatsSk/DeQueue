package com.dequeue.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePermissionRequest {

    @NotBlank
    private String resource;

    @NotBlank
    private String action;

    private String description;
}
