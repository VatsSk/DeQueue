package com.dequeue.staff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateStaffRequest {

    @NotBlank
    private String name;

    private String phone;

    private List<String> departmentIds = new ArrayList<>();

    private List<String> roleIds;
}
