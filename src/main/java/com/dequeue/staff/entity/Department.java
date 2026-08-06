package com.dequeue.staff.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "departments")
public class Department {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String name;
    private String description;
    
    @Builder.Default
    private boolean active = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
