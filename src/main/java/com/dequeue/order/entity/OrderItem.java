package com.dequeue.order.entity;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String menuItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private List<SelectedCustomization> selectedCustomizations;
    private String specialInstructions;
}
