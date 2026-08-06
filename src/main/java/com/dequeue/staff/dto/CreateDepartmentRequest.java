package com.dequeue.staff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartmentRequest {
    @NotBlank
    private String name;
    private String description;
}
