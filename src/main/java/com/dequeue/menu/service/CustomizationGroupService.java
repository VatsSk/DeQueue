package com.dequeue.menu.service;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import java.util.List;

public interface CustomizationGroupService {
    List<CustomizationGroupResponse> getCustomizationGroups();
    CustomizationGroupResponse getCustomizationGroup(String id);
    CustomizationGroupResponse createCustomizationGroup(CreateCustomizationGroupRequest request);
    CustomizationGroupResponse updateCustomizationGroup(String id, UpdateCustomizationGroupRequest request);
    void deleteCustomizationGroup(String id);
}
