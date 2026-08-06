package com.dequeue.reports.dto;

import lombok.Data;
import java.util.List;

@Data
public class PeakHourReport {
    private List<HourlyData> hours;
}
