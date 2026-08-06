package com.dequeue.settings.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.dequeue.settings.entity.VendorSetting;
import java.util.Optional;

@Repository
public interface SettingsRepository extends MongoRepository<VendorSetting, String> {
    Optional<VendorSetting> findByVendorId(String vendorId);
}
