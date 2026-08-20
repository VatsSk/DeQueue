package com.dequeue.settlement.repository;

import com.dequeue.settlement.entity.SettlementStatus;
import com.dequeue.settlement.entity.VendorSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorSettlementRepository extends MongoRepository<VendorSettlement, String> {

    List<VendorSettlement> findByVendorIdOrderByCreatedAtDesc(String vendorId);

    Page<VendorSettlement> findByVendorIdOrderByCreatedAtDesc(String vendorId, Pageable pageable);

    Optional<VendorSettlement> findBySettlementRef(String settlementRef);

    /**
     * Find the most recent SETTLED settlement for a vendor — used to determine "Settled Till" date.
     */
    Optional<VendorSettlement> findTopByVendorIdAndSettlementStatusOrderByPeriodToDesc(
            String vendorId, SettlementStatus status);

    List<VendorSettlement> findByVendorIdAndSettlementStatus(String vendorId, SettlementStatus status);

    boolean existsByVendorIdAndPeriodFromAndPeriodTo(String vendorId, LocalDate periodFrom, LocalDate periodTo);

    long countByVendorId(String vendorId);

    /** Used to generate sequential settlement ref numbers. */
    long countByVendorIdAndSettlementStatus(String vendorId, SettlementStatus status);
}
