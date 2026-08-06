package com.dequeue.reports.dto;

import lombok.Data;
import java.util.List;

@Data
public class PopularItemReport {
    private List<PopularItem> items;
}
