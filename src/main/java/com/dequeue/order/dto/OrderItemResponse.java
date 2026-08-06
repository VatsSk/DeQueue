package com.dequeue.order.dto;

import com.dequeue.order.entity.SelectedCustomization;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemResponse {
    private String menuItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private List<SelectedCustomization> selectedCustomizations;
    private String specialInstructions;
}
