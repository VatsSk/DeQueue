package com.dequeue.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateStaffRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    private List<String> departmentIds = new ArrayList<>();

    @NotEmpty(message = "At least one role must be assigned")
    private List<String> roleIds;
}
