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
        
        return vendor.getShopStatus();
    }

    @Override
    public ShopStatus getShopStatus(String userId) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        return vendor.getShopStatus();
    }

    @Override
    @Transactional
    public VendorSettings updateSettings(String userId, VendorSettingsDto request) {
        Vendor vendor = vendorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        
        if (vendor.getSettings() == null) {
            vendor.setSettings(new VendorSettings());
        }
        
        vendorMapper.updateSettingsFromDto(request, vendor.getSettings());
        vendorRepository.save(vendor);
        return vendor.getSettings();
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

    // Platform Admin Methods

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
        return vendorMapper.toResponse(vendor);
    }
}
