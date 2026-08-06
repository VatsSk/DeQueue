package com.dequeue.menu.repository;
import com.dequeue.menu.entity.CustomizationGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CustomizationGroupRepository extends MongoRepository<CustomizationGroup, String> {
    List<CustomizationGroup> findByVendorId(String vendorId);
    List<CustomizationGroup> findByIdIn(List<String> ids);
    Optional<CustomizationGroup> findByIdAndVendorId(String id, String vendorId);
}
