package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.service.CustomizationGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customizations")
@RequiredArgsConstructor
public class CustomizationController {
    private final CustomizationGroupService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<List<CustomizationGroupResponse>> getGroups() {
        return ApiResponse.success(service.getCustomizationGroups());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<CustomizationGroupResponse> getGroup(@PathVariable String id) {
        return ApiResponse.success(service.getCustomizationGroup(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<CustomizationGroupResponse> createGroup(@Valid @RequestBody CreateCustomizationGroupRequest request) {
        return ApiResponse.success(service.createCustomizationGroup(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<CustomizationGroupResponse> updateGroup(@PathVariable String id, @Valid @RequestBody UpdateCustomizationGroupRequest request) {
        return ApiResponse.success(service.updateCustomizationGroup(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<Void> deleteGroup(@PathVariable String id) {
        service.deleteCustomizationGroup(id);
        return ApiResponse.success(null);
    }
}
