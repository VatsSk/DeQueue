package com.dequeue.order.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Integer rating;
    private String feedback;
}
