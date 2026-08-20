package com.dequeue.settlement.repository;

import com.dequeue.settlement.entity.PaymentSource;
import com.dequeue.settlement.entity.PaymentStatus;
import com.dequeue.settlement.entity.PaymentTransaction;
import com.dequeue.settlement.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends MongoRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    boolean existsByOrderIdAndPaymentStatus(String orderId, PaymentStatus paymentStatus);

    List<PaymentTransaction> findByVendorId(String vendorId);

    List<PaymentTransaction> findByVendorIdAndSettlementStatus(String vendorId, SettlementStatus status);

    List<PaymentTransaction> findByVendorIdAndRecordedAtBetween(String vendorId, Instant start, Instant end);

    List<PaymentTransaction> findByVendorIdAndPaymentSourceAndRecordedAtBetween(
            String vendorId, PaymentSource source, Instant start, Instant end);

    List<PaymentTransaction> findByVendorIdAndSettlementStatusAndRecordedAtBetween(
            String vendorId, SettlementStatus settlementStatus, Instant start, Instant end);

    Page<PaymentTransaction> findByVendorId(String vendorId, Pageable pageable);

    Page<PaymentTransaction> findByVendorIdAndSettlementStatus(
            String vendorId, SettlementStatus status, Pageable pageable);

    List<PaymentTransaction> findBySettlementId(String settlementId);

    long countByVendorIdAndSettlementStatus(String vendorId, SettlementStatus status);
}
