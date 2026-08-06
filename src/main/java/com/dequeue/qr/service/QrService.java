package com.dequeue.qr.service;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;

public interface QrService {
    QrResponse getQrMetadata();
    QrResponse generateQr(QrGenerateRequest request);
    byte[] downloadQr();
}
