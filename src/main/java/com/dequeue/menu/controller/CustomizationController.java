package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.service.CustomizationGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customizations")
@RequiredArgsConstructor
public class CustomizationController {
    private final CustomizationGroupService service;

    @GetMapping
    public ApiResponse<List<CustomizationGroupResponse>> getGroups() {
        return ApiResponse.success(service.getCustomizationGroups());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomizationGroupResponse> getGroup(@PathVariable String id) {
        return ApiResponse.success(service.getCustomizationGroup(id));
    }

    @PostMapping
    public ApiResponse<CustomizationGroupResponse> createGroup(@Valid @RequestBody CreateCustomizationGroupRequest request) {
        return ApiResponse.success(service.createCustomizationGroup(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomizationGroupResponse> updateGroup(@PathVariable String id, @Valid @RequestBody UpdateCustomizationGroupRequest request) {
        return ApiResponse.success(service.updateCustomizationGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable String id) {
        service.deleteCustomizationGroup(id);
        return ApiResponse.success(null);
    }
}
