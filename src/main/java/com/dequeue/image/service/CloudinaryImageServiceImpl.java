package com.dequeue.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dequeue.image.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Uploads images to Cloudinary and returns their CDN URLs.
 * Credentials are loaded from application.yml / environment variables.
 */
@Slf4j
@Service
public class CloudinaryImageServiceImpl implements ImageService {

    private final Cloudinary cloudinary;

    public CloudinaryImageServiceImpl(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
        log.info("Cloudinary initialized with cloud: {}", cloudName);
    }

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, String folder) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",          "dequeue/" + (folder != null ? folder : "menu"),
                        "resource_type",   "image",
                        "transformation",  "q_auto,f_auto,w_800,c_limit"
                )
        );

        String url      = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");

        log.info("Uploaded image to Cloudinary: publicId={}, url={}", publicId, url);
        return ImageUploadResponse.builder()
                .url(url)
                .publicId(publicId)
                .build();
    }

    @Override
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        log.info("Deleted image from Cloudinary: publicId={}", publicId);
    }
}
