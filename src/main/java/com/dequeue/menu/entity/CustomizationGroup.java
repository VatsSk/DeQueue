package com.dequeue.menu.entity;

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
@Document(collection = "customization_groups")
public class CustomizationGroup {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String name;
    private SelectionType selectionType;
    
    @Builder.Default
    private boolean required = false;
    
    private Integer minSelection;
    private Integer maxSelection;
    private List<CustomizationOption> options;
    
    @Builder.Default
    private int sortOrder = 0;
    
    @Builder.Default
    private boolean active = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
