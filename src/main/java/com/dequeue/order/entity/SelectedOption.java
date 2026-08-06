package com.dequeue.order.entity;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedOption {
    private String name;
    private BigDecimal additionalPrice;
}
