package com.dequeue.geofence.service;

import com.dequeue.geofence.dto.*;

public interface GeofenceService {
    GeofenceResponse getGeofenceSettings(String vendorId);
    GeofenceResponse updateGeofenceSettings(String vendorId, UpdateGeofenceRequest request);
    ValidateLocationResponse validateLocation(ValidateLocationRequest request);
}
