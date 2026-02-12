package com.example.shopapi.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        validateImageFile(file);

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "transformation", ObjectUtils.asMap(
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        String imageUrl = (String) uploadResult.get("secure_url");

        log.info("Image uploaded successfully: {}", imageUrl);
        return imageUrl;
    }

    public String uploadImage(MultipartFile file, String customFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        validateImageFile(file);

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder + "/" + customFolder,
                "resource_type", "image",
                "transformation", ObjectUtils.asMap(
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        String imageUrl = (String) uploadResult.get("secure_url");

        log.info("Image uploaded successfully to {}: {}", customFolder, imageUrl);
        return imageUrl;
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted: {} - Result: {}", publicId, result.get("result"));
            }
        } catch (Exception e) {
            log.error("Error deleting image {}: {}", imageUrl, e.getMessage());
        }
    }

    public String updateImage(String oldImageUrl, MultipartFile newFile) throws IOException {
        deleteImage(oldImageUrl);
        return uploadImage(newFile);
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }

        // Check allowed extensions
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!isAllowedExtension(extension)) {
                throw new IllegalArgumentException("File type not allowed. Allowed types: jpg, jpeg, png, gif, webp");
            }
        }
    }

    private boolean isAllowedExtension(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") ||
               extension.equals("png") || extension.equals("gif") ||
               extension.equals("webp");
    }

    private String extractPublicId(String imageUrl) {
        try {
            // Extract public_id from Cloudinary URL
            // Format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{extension}
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String pathWithVersion = parts[1];
                // Remove version if present (v1234567890/)
                if (pathWithVersion.matches("v\\d+/.*")) {
                    pathWithVersion = pathWithVersion.substring(pathWithVersion.indexOf("/") + 1);
                }
                // Remove file extension
                int lastDot = pathWithVersion.lastIndexOf(".");
                if (lastDot > 0) {
                    return pathWithVersion.substring(0, lastDot);
                }
                return pathWithVersion;
            }
        } catch (Exception e) {
            log.error("Error extracting public ID from URL {}: {}", imageUrl, e.getMessage());
        }
        return null;
    }
}

