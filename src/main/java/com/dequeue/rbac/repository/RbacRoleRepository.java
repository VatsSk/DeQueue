package com.dequeue.rbac.repository;

import com.dequeue.rbac.entity.RbacRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RbacRoleRepository extends MongoRepository<RbacRole, String> {

    List<RbacRole> findByActiveTrue();

    Optional<RbacRole> findByName(String name);

    boolean existsByName(String name);

    List<RbacRole> findByNameIn(List<String> names);

    List<RbacRole> findByIdIn(List<String> ids);
}
