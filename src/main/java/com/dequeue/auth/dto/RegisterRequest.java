package com.dequeue.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String shopName;
    @NotBlank
    private String ownerName;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String phone;
    @NotBlank
    private String address;
}
