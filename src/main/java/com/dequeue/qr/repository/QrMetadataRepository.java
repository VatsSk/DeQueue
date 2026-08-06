package com.dequeue.qr.repository;
import com.dequeue.qr.entity.QrMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface QrMetadataRepository extends MongoRepository<QrMetadata, String> {
    Optional<QrMetadata> findByVendorId(String vendorId);
    Optional<QrMetadata> findByVendorCode(String vendorCode);
}
