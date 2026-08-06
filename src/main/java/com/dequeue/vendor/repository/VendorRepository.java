package com.dequeue.vendor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.dequeue.vendor.entity.Vendor;
import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository<Vendor, String> {
    Optional<Vendor> findByVendorCode(String vendorCode);
    Optional<Vendor> findByEmail(String email);
    boolean existsByVendorCode(String vendorCode);
}
