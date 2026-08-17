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

    // ────────────────────────────────────────────────────────────────────────
    // Platform Admin Methods
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public java.util.List<VendorResponse> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(vendorMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public VendorResponse toggleVendorStatus(String vendorId, boolean active) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setActive(active);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request) {
        // We will just create the Vendor. The frontend for Platform Admin
        // can use this to onboard a new vendor.
        String vendorCode = request.getShopName().toLowerCase().replaceAll("[^a-z0-9]", "-")
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 4);

        Vendor vendor = new Vendor();
        vendor.setShopName(request.getShopName());
        vendor.setOwnerName(request.getOwnerName());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(com.dequeue.vendor.entity.Address.builder().street(request.getAddress()).build());
        vendor.setVendorCode(vendorCode);
        vendor.setShopStatus(ShopStatus.CLOSED);
        vendor.setActive(true);
        
        vendor = vendorRepository.save(vendor);

        // Note: Creating the initial admin user and roles should ideally be here or via a separate endpoint,
        // but for simplicity we return the created vendor. In a real scenario, this would reuse the 
        // logic from AuthServiceImpl.register or publish an event.
        
        return vendorMapper.toResponse(vendor);
    }
}
