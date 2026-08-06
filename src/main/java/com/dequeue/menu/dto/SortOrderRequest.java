package com.dequeue.menu.dto;
import lombok.Data;
import java.util.List;

@Data
public class SortOrderRequest {
    private List<SortOrderItem> items;
}
