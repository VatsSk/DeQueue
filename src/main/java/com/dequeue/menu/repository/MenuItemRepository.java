package com.dequeue.menu.repository;
import com.dequeue.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    Page<MenuItem> findByVendorId(String vendorId, Pageable pageable);
    Page<MenuItem> findByVendorIdAndCategoryId(String vendorId, String categoryId, Pageable pageable);
    List<MenuItem> findByVendorIdAndAvailableAndVisible(String vendorId, boolean available, boolean visible);
    long countByVendorIdAndCategoryId(String vendorId, String categoryId);
    Optional<MenuItem> findByIdAndVendorId(String id, String vendorId);
    List<MenuItem> findByCategoryId(String categoryId);
}
