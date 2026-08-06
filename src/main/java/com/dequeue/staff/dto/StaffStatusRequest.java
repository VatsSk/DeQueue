package com.dequeue.staff.dto;

import com.dequeue.staff.entity.StaffStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffStatusRequest {
    @NotNull
    private StaffStatus status;
}
