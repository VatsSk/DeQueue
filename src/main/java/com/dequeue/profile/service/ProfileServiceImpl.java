package com.dequeue.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dequeue.profile.dto.*;
import com.dequeue.profile.entity.VendorProfile;
import com.dequeue.profile.repository.ProfileRepository;
import com.dequeue.profile.mapper.ProfileMapper;
import com.dequeue.common.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Override
    public ProfileResponse getProfile(String vendorId) {
        VendorProfile profile = profileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for vendor"));
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String vendorId, UpdateProfileRequest request) {
        VendorProfile profile = profileRepository.findByVendorId(vendorId)
                .orElse(new VendorProfile());
        
        if (profile.getId() == null) {
            profile.setVendorId(vendorId);
        }
        
        profileMapper.updateProfileFromRequest(request, profile);
        profile = profileRepository.save(profile);
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse uploadLogo(String vendorId, MultipartFile file) {
        VendorProfile profile = profileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        
        // TODO: implement image service upload
        // String logoUrl = imageService.uploadImage(file);
        // profile.setLogoUrl(logoUrl);
        profile = profileRepository.save(profile);
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse uploadBanner(String vendorId, MultipartFile file) {
        VendorProfile profile = profileRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        
        // TODO: implement image service upload
        // String bannerUrl = imageService.uploadImage(file);
        // profile.setBannerUrl(bannerUrl);
        profile = profileRepository.save(profile);
        return profileMapper.toResponse(profile);
    }
}
