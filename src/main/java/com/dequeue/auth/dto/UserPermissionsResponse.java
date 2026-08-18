package com.dequeue.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserPermissionsResponse {
    private String userId;
    private List<String> roles;
    private List<String> permissions;
}