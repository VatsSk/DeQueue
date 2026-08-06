package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuItemSummary {
    private String id;
    private String name;
    private BigDecimal price;
    private String image;
    private Boolean available;
    private String categoryId;
}
