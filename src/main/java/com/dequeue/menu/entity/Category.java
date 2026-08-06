package com.dequeue.menu.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categories")
public class Category {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String name;
    private String description;
    private String image;
    
    @Builder.Default
    private int sortOrder = 0;
    
    @Builder.Default
    private boolean active = true;
    
    @Builder.Default
    private int itemCount = 0;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
