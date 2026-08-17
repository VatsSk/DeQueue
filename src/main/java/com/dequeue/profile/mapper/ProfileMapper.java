package com.dequeue.profile.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.dequeue.profile.dto.*;
import com.dequeue.profile.entity.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {
    @org.mapstruct.Mapping(source = "logo", target = "logoUrl")
    @org.mapstruct.Mapping(source = "banner", target = "bannerUrl")
    ProfileResponse toResponse(VendorProfile profile);
    void updateProfileFromRequest(UpdateProfileRequest request, @MappingTarget VendorProfile profile);
}
