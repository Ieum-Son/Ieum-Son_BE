package org.gh7035.ieumson.infrastructure.s3;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3FileUploader {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public String uploadProfileImage(String loginId, MultipartFile file) {
        validateImage(file);

        String extension = resolveExtension(file);
        String key = s3Properties.profileImagePrefix() + "/" + loginId + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | S3Exception e) {
            throw new IeumException(ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED, e);
        }

        return buildObjectUrl(key);
    }

    public void deleteIfOwned(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }

        String prefix = buildObjectUrl("");
        if (!imageUrl.startsWith(prefix)) {
            return;
        }

        String key = imageUrl.substring(prefix.length());
        if (!StringUtils.hasText(key)) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build());
        } catch (S3Exception ignored) {
            // 이전 이미지 삭제 실패는 업로드 성공을 막지 않음
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IeumException(ErrorCode.PROFILE_IMAGE_REQUIRED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IeumException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (extension.matches("\\.(jpe?g|png|webp|gif)")) {
                return extension.equals(".jpeg") ? ".jpg" : extension;
            }
        }

        return switch (file.getContentType().toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String buildObjectUrl(String key) {
        return "https://" + s3Properties.bucket() + ".s3." + s3Properties.region() + ".amazonaws.com/" + key;
    }
}
