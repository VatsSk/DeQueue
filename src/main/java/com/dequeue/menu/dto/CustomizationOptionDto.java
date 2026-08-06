package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomizationOptionDto {
    private String name;
    private BigDecimal additionalPrice;
    private Boolean available;
    private Integer sortOrder;
}
