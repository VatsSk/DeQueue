package com.dequeue.menu.dto;
import lombok.Data;
import java.util.List;

@Data
public class PublicMenuResponse {
    private String vendorCode;
    private String shopName;
    private List<CategoryWithItemsResponse> categories;
}
