package org.gh7035.ieumson.infrastructure.s3;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3FileManager {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/x-png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public String uploadProfileImage(String loginId, MultipartFile file) {
        String contentType = resolveContentType(file);
        String extension = resolveExtension(file, contentType);
        String key = s3Properties.profileImagePrefix() + "/" + loginId + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(contentType)
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


    private String resolveContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IeumException(ErrorCode.PROFILE_IMAGE_REQUIRED);
        }

        String headerType = normalizeContentType(file.getContentType());
        if (ALLOWED_CONTENT_TYPES.contains(headerType)) {
            return canonicalContentType(headerType);
        }

        String extensionType = contentTypeFromExtension(file.getOriginalFilename());
        if (extensionType != null) {
            return extensionType;
        }

        String magicType = contentTypeFromMagicBytes(file);
        if (magicType != null) {
            return magicType;
        }

        throw new IeumException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE);
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        int separator = contentType.indexOf(';');
        String type = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private static String canonicalContentType(String contentType) {
        return switch (contentType) {
            case "image/jpg", "image/jpeg" -> "image/jpeg";
            case "image/x-png" -> "image/png";
            default -> contentType;
        };
    }

    private static String contentTypeFromExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> null;
        };
    }

    private static String contentTypeFromMagicBytes(MultipartFile file) {
        try {
            byte[] header = file.getBytes();
            if (header.length >= 8
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47) {
                return "image/png";
            }
            if (header.length >= 3
                    && header[0] == (byte) 0xFF
                    && header[1] == (byte) 0xD8
                    && header[2] == (byte) 0xFF) {
                return "image/jpeg";
            }
            if (header.length >= 6
                    && header[0] == 'G'
                    && header[1] == 'I'
                    && header[2] == 'F') {
                return "image/gif";
            }
            if (header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P') {
                return "image/webp";
            }
            return null;
        } catch (IOException e) {
            throw new IeumException(ErrorCode.INVALID_PROFILE_IMAGE_TYPE, e);
        }
    }

    private String resolveExtension(MultipartFile file, String contentType) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(extension)) {
                return extension.equals(".jpeg") ? ".jpg" : extension;
            }
        }

        return switch (contentType) {
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
