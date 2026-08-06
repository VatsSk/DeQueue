package com.dequeue.image.service;
import com.dequeue.image.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ImageService {
    ImageUploadResponse uploadImage(MultipartFile file, String folder) throws IOException;
    void deleteImage(String publicId) throws IOException;
}
