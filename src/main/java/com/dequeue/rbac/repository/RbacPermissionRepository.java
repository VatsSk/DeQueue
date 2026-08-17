package com.dequeue.rbac.repository;

import com.dequeue.rbac.entity.RbacPermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RbacPermissionRepository extends MongoRepository<RbacPermission, String> {

    List<RbacPermission> findByActiveTrue();

    Optional<RbacPermission> findByResourceAndAction(String resource, String action);

    List<RbacPermission> findByIdIn(List<String> ids);
}
