package com.dequeue.menu.dto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryWithItemsResponse extends CategoryResponse {
    private List<MenuItemResponse> items;
}
