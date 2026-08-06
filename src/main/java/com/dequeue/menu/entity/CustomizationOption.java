package com.dequeue.menu.entity;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationOption {
    private String name;
    @Builder.Default
    private BigDecimal additionalPrice = BigDecimal.ZERO;
    @Builder.Default
    private boolean available = true;
    @Builder.Default
    private int sortOrder = 0;
}
