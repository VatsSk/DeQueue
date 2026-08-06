package com.dequeue.profile.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.dequeue.profile.dto.*;
import com.dequeue.profile.entity.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {
    ProfileResponse toResponse(VendorProfile profile);
    void updateProfileFromRequest(UpdateProfileRequest request, @MappingTarget VendorProfile profile);
}
