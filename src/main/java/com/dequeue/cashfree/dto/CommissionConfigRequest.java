package com.dequeue.cashfree.dto;

import com.dequeue.cashfree.entity.CommissionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionConfigRequest {
    @NotNull
    private CommissionType commissionType;

    @NotNull
    @Positive
    private BigDecimal commissionRate;
}
