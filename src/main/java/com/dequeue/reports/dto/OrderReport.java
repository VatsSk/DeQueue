package com.dequeue.reports.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderReport {
    private String dateRange;
    private int totalOrders;
    private Map<OrderStatus, Long> byStatus;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
}
