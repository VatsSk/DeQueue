package com.dequeue.menu.dto;
import lombok.Data;

@Data
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private String image;
    private Integer sortOrder;
    private Boolean active;
    private Long itemCount;
}
