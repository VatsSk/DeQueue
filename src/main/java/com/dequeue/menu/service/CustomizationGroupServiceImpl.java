package com.dequeue.menu.service;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.entity.CustomizationGroup;
import com.dequeue.menu.mapper.CustomizationGroupMapper;
import com.dequeue.menu.repository.CustomizationGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomizationGroupServiceImpl implements CustomizationGroupService {
    private final CustomizationGroupRepository repository;
    private final CustomizationGroupMapper mapper;

    @Override
    public List<CustomizationGroupResponse> getCustomizationGroups() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return repository.findByVendorId(vendorId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomizationGroupResponse getCustomizationGroup(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return repository.findByIdAndVendorId(id, vendorId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    @Override
    public CustomizationGroupResponse createCustomizationGroup(CreateCustomizationGroupRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        CustomizationGroup group = mapper.toEntity(request);
        group.setVendorId(vendorId);
        return mapper.toResponse(repository.save(group));
    }

    @Override
    public CustomizationGroupResponse updateCustomizationGroup(String id, UpdateCustomizationGroupRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        CustomizationGroup group = repository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        mapper.updateEntity(request, group);
        return mapper.toResponse(repository.save(group));
    }

    @Override
    public void deleteCustomizationGroup(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        CustomizationGroup group = repository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        repository.delete(group);
    }
}
