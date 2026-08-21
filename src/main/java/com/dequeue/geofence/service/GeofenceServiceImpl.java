package com.dequeue.geofence.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.dequeue.geofence.dto.*;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.entity.GeoLocation;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.common.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeofenceServiceImpl implements GeofenceService {

    private final VendorRepository vendorRepository;

    @Override
    public GeofenceResponse getGeofenceSettings(String vendorId) {
        if (vendorId == null) {
            throw new com.dequeue.common.exception.BadRequestException("Vendor ID is missing from current security context");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
                
        GeofenceResponse response = new GeofenceResponse();
        if (vendor.getGeoLocation() != null) {
            response.setLatitude(vendor.getGeoLocation().getLatitude());
            response.setLongitude(vendor.getGeoLocation().getLongitude());
        }
        response.setRadius(vendor.getGeoRadius() != null ? vendor.getGeoRadius() : 500.0);
        boolean enabled = vendor.getSettings() != null && vendor.getSettings().isEnableGeofence();
        response.setEnabled(enabled);
        
        return response;
    }

    @Override
    public GeofenceResponse updateGeofenceSettings(String vendorId, UpdateGeofenceRequest request) {
        if (vendorId == null) {
            throw new com.dequeue.common.exception.BadRequestException("Vendor ID is missing from current security context");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
                
        GeoLocation location = new GeoLocation();
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        vendor.setGeoLocation(location);
        vendor.setGeoRadius(request.getRadius());

        // Persist the enabled flag in VendorSettings
        com.dequeue.vendor.entity.VendorSettings settings = vendor.getSettings();
        if (settings == null) {
            settings = new com.dequeue.vendor.entity.VendorSettings();
        }
        settings.setEnableGeofence(request.isEnabled());
        vendor.setSettings(settings);
        
        vendorRepository.save(vendor);
        
        GeofenceResponse response = new GeofenceResponse();
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setRadius(request.getRadius());
        response.setEnabled(request.isEnabled());
        
        return response;
    }

    @Override
    public ValidateLocationResponse validateLocation(ValidateLocationRequest request) {
        Vendor vendor = vendorRepository.findByVendorCode(request.getVendorCode())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        
        // If geofence is not enabled or not configured, allow ordering
        boolean geofenceEnabled = vendor.getSettings() != null && vendor.getSettings().isEnableGeofence();
        if (!geofenceEnabled || vendor.getGeoLocation() == null || vendor.getGeoRadius() == null) {
            return new ValidateLocationResponse(true, 0.0, 0.0);
        }
        
        double distance = calculateDistance(
            vendor.getGeoLocation().getLatitude(), 
            vendor.getGeoLocation().getLongitude(),
            request.getCustomerLatitude(), 
            request.getCustomerLongitude()
        );
        
        boolean withinRange = distance <= vendor.getGeoRadius();
        return new ValidateLocationResponse(withinRange, distance, vendor.getGeoRadius());
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c * 1000; // convert to meters
    }
}
