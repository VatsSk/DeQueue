package com.dequeue.profile.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.dequeue.profile.entity.VendorProfile;
import java.util.Optional;

@Repository
public interface ProfileRepository extends MongoRepository<VendorProfile, String> {
    Optional<VendorProfile> findByVendorId(String vendorId);
}
