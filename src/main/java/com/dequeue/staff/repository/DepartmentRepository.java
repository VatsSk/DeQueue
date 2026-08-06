package com.dequeue.staff.repository;

import com.dequeue.staff.entity.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {
    List<Department> findByVendorId(String vendorId);
    Optional<Department> findByVendorIdAndName(String vendorId, String name);
}
