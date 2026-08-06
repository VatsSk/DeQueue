package com.dequeue.auth.repository;

import com.dequeue.auth.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByStaffId(String staffId);
    void deleteByToken(String token);
}
