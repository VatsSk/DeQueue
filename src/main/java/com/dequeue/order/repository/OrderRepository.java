package com.dequeue.order.repository;

import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dequeue.settlement.entity.SettlementStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByVendorId(String vendorId, Pageable pageable);
    Page<Order> findByVendorIdAndStatus(String vendorId, OrderStatus status, Pageable pageable);
    Page<Order> findByVendorIdAndQueueNumber(String vendorId, String queueNumber, Pageable pageable);
    Optional<Order> findByVendorIdAndQueueNumber(String vendorId, String queueNumber);
    Optional<Order> findByCashfreeOrderId(String cashfreeOrderId);
    List<Order> findByVendorIdAndStatusIn(String vendorId, List<OrderStatus> statuses);
    Page<Order> findByVendorIdAndStatusIn(String vendorId, List<OrderStatus> statuses, Pageable pageable);
    List<Order> findByVendorIdAndCreatedAtAfter(String vendorId, Instant date);
    long countByVendorIdAndStatus(String vendorId, OrderStatus status);
    List<Order> findByVendorIdAndCreatedAtBetween(String vendorId, Instant start, Instant end);

    // Settlement-related queries
    List<Order> findByVendorIdAndStatus(String vendorId, OrderStatus status);
    List<Order> findBySettlementId(String settlementId);
    List<Order> findByVendorIdAndSettlementId(String vendorId, String settlementId);
    List<Order> findByVendorIdAndSettlementStatus(
            String vendorId, SettlementStatus settlementStatus);
    List<Order> findByVendorIdAndStatusAndSettlementStatus(
            String vendorId, OrderStatus status, SettlementStatus settlementStatus);
    List<Order> findByVendorIdAndStatusAndCreatedAtBetween(
            String vendorId, OrderStatus status, Instant start, Instant end);
}

