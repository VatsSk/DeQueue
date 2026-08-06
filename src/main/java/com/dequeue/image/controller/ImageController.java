package com.dequeue.image.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.image.dto.ImageUploadResponse;
import com.dequeue.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;
    
    @PostMapping("/upload")
    public ApiResponse<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file, @RequestParam("folder") String folder) throws IOException {
        return ApiResponse.success(imageService.uploadImage(file, folder));
    }
    
    @DeleteMapping("/{publicId}")
    public ApiResponse<Void> delete(@PathVariable String publicId) throws IOException {
        imageService.deleteImage(publicId);
        return ApiResponse.success(null);
    }
}
