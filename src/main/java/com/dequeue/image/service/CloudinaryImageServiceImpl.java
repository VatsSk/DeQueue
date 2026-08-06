package com.dequeue.image.service;
import com.dequeue.image.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CloudinaryImageServiceImpl implements ImageService {
    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, String folder) throws IOException {
        return ImageUploadResponse.builder()
                .url("https://res.cloudinary.com/demo/image/upload/v1/sample.jpg")
                .publicId("sample")
                .build();
    }

    @Override
    public void deleteImage(String publicId) throws IOException {
    }
}
