package com.dequeue.rbac.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PermissionResponse {
    private String id;
    private String resource;
    private String action;
    private String permissionKey; // "resource.action"
    private String description;
    private boolean active;
    private Instant createdAt;
}
