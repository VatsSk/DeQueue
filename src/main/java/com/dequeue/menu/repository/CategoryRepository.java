package com.dequeue.menu.repository;
import com.dequeue.menu.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByVendorId(String vendorId);
    List<Category> findByVendorIdAndActive(String vendorId, boolean active);
    Optional<Category> findByIdAndVendorId(String id, String vendorId);
}
