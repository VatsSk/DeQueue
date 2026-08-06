package com.dequeue.staff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDepartmentRequest {
    @NotBlank
    private String name;
    private String description;
}
