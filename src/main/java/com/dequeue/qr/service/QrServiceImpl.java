package com.dequeue.qr.service;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService {
    private final VendorRepository vendorRepository;

    private QrResponse generateResponse() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        var vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        String vendorCode = vendor.getVendorCode();
        String url = "http://localhost:8080/customer.html?vendor=" + vendorCode;
        
        QrResponse response = new QrResponse();
        response.setVendorCode(vendorCode);
        response.setQrUrl(url);
        // Using an external QR API for instant QR code generation
        response.setQrImageUrl("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + url);
        response.setGeneratedAt(Instant.now());
        response.setDownloadCount(0);
        return response;
    }

    @Override
    public QrResponse getQrMetadata() {
        return generateResponse();
    }

    @Override
    public QrResponse generateQr(QrGenerateRequest request) {
        return generateResponse();
    }

    @Override
    public byte[] downloadQr() {
        return new byte[0]; // Can be implemented if needed
    }
}
