package com.dequeue.profile.service;

import com.dequeue.profile.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    ProfileResponse getProfile(String vendorId);
    ProfileResponse updateProfile(String vendorId, UpdateProfileRequest request);
    ProfileResponse uploadLogo(String vendorId, MultipartFile file);
    ProfileResponse uploadBanner(String vendorId, MultipartFile file);
}
