package com.dequeue.staff.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staff")
public class Staff {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String name;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    private String phone;
    private String departmentId;
    private String departmentName;
    private Role role;
    private List<Permission> permissions;
    private StaffStatus status;
    private String avatar;
    private Instant lastLoginAt;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
