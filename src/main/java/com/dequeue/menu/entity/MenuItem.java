package com.dequeue.menu.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "menu_items")
public class MenuItem {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    @Indexed
    private String categoryId;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    
    @Builder.Default
    private boolean available = true;
    
    private Integer preparationTime;
    
    @Builder.Default
    private int sortOrder = 0;
    
    @Builder.Default
    private boolean visible = true;
    
    private List<String> customizationGroups;
    private List<String> tags;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
