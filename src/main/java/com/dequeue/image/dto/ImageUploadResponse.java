package com.dequeue.image.dto;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ImageUploadResponse {
    private String url;
    private String publicId;
    private Integer width;
    private Integer height;
    private String format;
    private Long bytes;
}
