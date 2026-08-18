package com.dequeue.rbac.repository;

import com.dequeue.rbac.entity.RbacRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RbacRoleRepository extends MongoRepository<RbacRole, String> {

    List<RbacRole> findByVendorIdAndActiveTrue(String vendorId);

    List<RbacRole> findByIdInAndVendorId(List<String> ids, String vendorId);

    List<RbacRole> findByIdIn(List<String> ids);

    boolean existsByVendorIdAndName(String vendorId, String name);

    List<RbacRole> findByVendorId(String vendorId);

    List<RbacRole> findByVendorIdAndNameIn(String vendorId, List<String> names);
}
