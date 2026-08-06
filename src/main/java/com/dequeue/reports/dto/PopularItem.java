package com.dequeue.reports.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PopularItem {
    private String menuItemId;
    private String menuItemName;
    private int orderCount;
    private BigDecimal totalRevenue;
}
