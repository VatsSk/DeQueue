package com.dequeue.staff.repository;

import com.dequeue.staff.entity.Staff;
import com.dequeue.staff.entity.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends MongoRepository<Staff, String> {
    Page<Staff> findByVendorId(String vendorId, Pageable pageable);
    List<Staff> findByVendorId(String vendorId);
    List<Staff> findByVendorIdAndDepartmentIdsContaining(String vendorId, String departmentId);
    Optional<Staff> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByVendorIdAndDepartmentIdsContaining(String vendorId, String departmentId);
    List<Staff> findByVendorIdAndStatus(String vendorId, StaffStatus status);
}
