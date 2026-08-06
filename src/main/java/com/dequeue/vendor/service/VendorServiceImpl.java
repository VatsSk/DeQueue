package com.dequeue.vendor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.*;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.vendor.mapper.VendorMapper;
import com.dequeue.common.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    public VendorResponse getCurrentVendor(String userId) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return vendorMapper.toResponse(vendor);
    }

    @Override
    @Transactional
    public VendorResponse updateVendor(String userId, UpdateVendorRequest request) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        
        vendorMapper.updateVendorFromRequest(request, vendor);
        vendor = vendorRepository.save(vendor);
        return vendorMapper.toResponse(vendor);
    }

    @Override
    @Transactional
    public ShopStatus updateShopStatus(String userId, ShopStatusRequest request) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        
        vendor.setShopStatus(request.getStatus());
        vendorRepository.save(vendor);
        
        // TODO: publish event for Redis cache invalidation and update Redis with online status
        
        return vendor.getShopStatus();
    }

    @Override
    public ShopStatus getShopStatus(String userId) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return vendor.getShopStatus();
    }

    @Override
    public PublicVendorResponse getVendorByCode(String vendorCode) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with code: " + vendorCode));
        return vendorMapper.toPublicResponse(vendor);
    }

    @Override
    public ShopStatus getVendorStatusByCode(String vendorCode) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with code: " + vendorCode));
        return vendor.getShopStatus();
    }
}
