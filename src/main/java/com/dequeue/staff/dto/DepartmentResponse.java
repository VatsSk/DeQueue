package com.dequeue.staff.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class DepartmentResponse {
    private String id;
    private String name;
    private String description;
    private int staffCount;
    private boolean active;
    private Instant createdAt;
}
