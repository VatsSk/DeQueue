package com.dequeue.rbac.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class RbacRole {

    @Id
    private String id;

    private String name;
    private String description;

    /** List of permission keys, e.g. ["menu.view", "menu.edit"] */
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    /** Controls which order statuses this role can see */
    @Builder.Default
    private OrderVisibility orderVisibility = new OrderVisibility();

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
