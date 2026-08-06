package com.dequeue.vendor.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.*;
import com.dequeue.vendor.entity.VendorSettings;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VendorMapper {
    VendorResponse toResponse(Vendor vendor);
    PublicVendorResponse toPublicResponse(Vendor vendor);
    void updateVendorFromRequest(UpdateVendorRequest request, @MappingTarget Vendor vendor);
    
    default PublicVendorResponse.PublicSettingsDto mapSettings(VendorSettings settings) {
        if (settings == null) return null;
        PublicVendorResponse.PublicSettingsDto dto = new PublicVendorResponse.PublicSettingsDto();
        dto.setAllowCustomOrder(settings.isAllowCustomOrder());
        dto.setShowPreparationTime(settings.isShowPreparationTime());
        dto.setCurrency(settings.getCurrency());
        return dto;
    }
}
