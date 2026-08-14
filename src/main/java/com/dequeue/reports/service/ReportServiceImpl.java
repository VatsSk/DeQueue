package com.dequeue.reports.service;

import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.entity.OrderItem;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.reports.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;

    private Instant getStartOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
    
    private Instant getEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
    }

    private List<Order> getOrders(String vendorId, LocalDate startDate, LocalDate endDate) {
        return orderRepository.findByVendorIdAndCreatedAtBetween(
            vendorId, getStartOfDay(startDate), getEndOfDay(endDate)
        );
    }
    
    @Override
    public TodayReport getTodayReport(String vendorId) {
        LocalDate today = LocalDate.now();
        List<Order> orders = getOrders(vendorId, today, today);
        
        TodayReport report = new TodayReport();
        report.setDate(today);
        report.setTotalOrders(orders.size());
        
        int completed = 0, pending = 0, cancelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        
        long totalPrepTime = 0;
        int prepTimeCount = 0;
        
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.COLLECTED) {
                completed++;
                revenue = revenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
                if (order.getCompletedAt() != null && order.getCreatedAt() != null) {
                    totalPrepTime += ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getCompletedAt());
                    prepTimeCount++;
                }
            } else if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelled++;
            } else {
                pending++;
            }
        }
        
        report.setCompletedOrders(completed);
        report.setPendingOrders(pending);
        report.setCancelledOrders(cancelled);
        report.setTotalRevenue(revenue);
        report.setAveragePrepTime(prepTimeCount > 0 ? (int)(totalPrepTime / prepTimeCount) : 0);
        
        report.setComparedToYesterday(new HashMap<>());
        
        return report;
    }
    
    @Override
    public OrderReport getOrderReport(String vendorId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getOrders(vendorId, startDate, endDate);
        
        OrderReport report = new OrderReport();
        report.setDateRange(startDate.toString() + " to " + endDate.toString());
        report.setTotalOrders(orders.size());
        
        Map<OrderStatus, Long> byStatus = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        for (Order order : orders) {
            byStatus.put(order.getStatus(), byStatus.getOrDefault(order.getStatus(), 0L) + 1);
            if (order.getStatus() == OrderStatus.COLLECTED) {
                totalRevenue = totalRevenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            }
        }
        
        report.setByStatus(byStatus);
        report.setTotalRevenue(totalRevenue);
        
        long completedCount = byStatus.getOrDefault(OrderStatus.COLLECTED, 0L);
        if (completedCount > 0) {
            report.setAverageOrderValue(totalRevenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP));
        } else {
            report.setAverageOrderValue(BigDecimal.ZERO);
        }
        
        return report;
    }
    
    @Override
    public PopularItemReport getPopularItems(String vendorId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getOrders(vendorId, startDate, endDate);
        
        Map<String, PopularItem> itemMap = new HashMap<>();
        
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            
            if (order.getOrderItems() != null) {
                for (OrderItem oi : order.getOrderItems()) {
                    String key = oi.getMenuItemId();
                    PopularItem item = itemMap.getOrDefault(key, new PopularItem());
                    item.setMenuItemId(key);
                    item.setMenuItemName(oi.getMenuItemName());
                    item.setOrderCount(item.getOrderCount() + oi.getQuantity());
                    
                    BigDecimal itemTotal = oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
                    item.setTotalRevenue(item.getTotalRevenue() == null ? itemTotal : item.getTotalRevenue().add(itemTotal));
                    
                    itemMap.put(key, item);
                }
            }
        }
        
        List<PopularItem> items = new ArrayList<>(itemMap.values());
        items.sort((a, b) -> Integer.compare(b.getOrderCount(), a.getOrderCount()));
        
        PopularItemReport report = new PopularItemReport();
        report.setItems(items);
        return report;
    }
    
    @Override
    public PeakHourReport getPeakHours(String vendorId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getOrders(vendorId, startDate, endDate);
        
        Map<Integer, HourlyData> hourMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            HourlyData hd = new HourlyData();
            hd.setHour(i);
            hd.setOrderCount(0);
            hd.setRevenue(BigDecimal.ZERO);
            hourMap.put(i, hd);
        }
        
        for (Order order : orders) {
            if (order.getCreatedAt() != null) {
                int hour = order.getCreatedAt().atZone(ZoneId.systemDefault()).getHour();
                HourlyData hd = hourMap.get(hour);
                hd.setOrderCount(hd.getOrderCount() + 1);
                if (order.getStatus() == OrderStatus.COLLECTED && order.getTotalAmount() != null) {
                    hd.setRevenue(hd.getRevenue().add(order.getTotalAmount()));
                }
            }
        }
        
        PeakHourReport report = new PeakHourReport();
        report.setHours(new ArrayList<>(hourMap.values()));
        return report;
    }
    
    @Override
    public QueueStatsReport getQueueStats(String vendorId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getOrders(vendorId, startDate, endDate);
        
        long totalWaitTime = 0;
        long totalPrepTime = 0;
        int completedCount = 0;
        
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.COLLECTED && order.getCreatedAt() != null && order.getCompletedAt() != null) {
                totalWaitTime += ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getCompletedAt());
                if (order.getPreparationStartedAt() != null) {
                    totalPrepTime += ChronoUnit.MINUTES.between(order.getPreparationStartedAt(), order.getCompletedAt());
                } else {
                    totalPrepTime += ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getCompletedAt());
                }
                completedCount++;
            }
        }
        
        QueueStatsReport report = new QueueStatsReport();
        report.setTotalServed(completedCount);
        if (completedCount > 0) {
            report.setAverageWaitTime((int) (totalWaitTime / completedCount));
            report.setAveragePrepTime((int) (totalPrepTime / completedCount));
        } else {
            report.setAverageWaitTime(0);
            report.setAveragePrepTime(0);
        }
        report.setMaxQueueLength(0);
        return report;
    }
    
    @Override
    public SummaryReport getSummary(String vendorId, LocalDate startDate, LocalDate endDate) {
        SummaryReport summary = new SummaryReport();
        summary.setOrderReport(getOrderReport(vendorId, startDate, endDate));
        summary.setPopularItemReport(getPopularItems(vendorId, startDate, endDate));
        summary.setPeakHourReport(getPeakHours(vendorId, startDate, endDate));
        summary.setQueueStatsReport(getQueueStats(vendorId, startDate, endDate));
        return summary;
    }
    
    @Override
    public String getExportCSV(String vendorId, LocalDate startDate, LocalDate endDate) {
        List<Order> orders = getOrders(vendorId, startDate, endDate);
        StringBuilder csv = new StringBuilder();
        csv.append("Order ID,Date,Status,Total Amount,Item Count\n");
        for (Order order : orders) {
            String date = order.getCreatedAt() != null ? order.getCreatedAt().toString() : "";
            int itemCount = order.getOrderItems() != null ? order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0;
            BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            csv.append(String.format("%s,%s,%s,%s,%d\n",
                    order.getId(),
                    date,
                    order.getStatus(),
                    amount,
                    itemCount
            ));
        }
        return csv.toString();
    }
}
