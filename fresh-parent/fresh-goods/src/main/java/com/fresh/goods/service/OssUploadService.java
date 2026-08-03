package com.fresh.goods.service;

import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.IdUtils;
import com.fresh.goods.config.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssUploadService {

    private static final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif");

    private final OssProperties ossProperties;

    public String uploadImage(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (!isAllowedImage(contentType, file.getOriginalFilename())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "仅支持 jpg/png/webp/gif 图片");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "图片大小不能超过 5MB");
        }
        if (ossProperties.isEnabled()) {
            return uploadToOss(file, dir);
        }
        return uploadToLocal(file, dir);
    }

    private boolean isAllowedImage(String contentType, String filename) {
        if (contentType != null && ALLOW_TYPES.contains(contentType)) {
            return true;
        }
        // 部分客户端/网关会把 Content-Type 变成 octet-stream，按后缀兜底
        String suffix = getSuffix(filename).toLowerCase();
        return ".jpg".equals(suffix) || ".jpeg".equals(suffix) || ".png".equals(suffix)
                || ".webp".equals(suffix) || ".gif".equals(suffix);
    }

    private String uploadToLocal(MultipartFile file, String dir) {
        try {
            String subDir = sanitizeDir(dir);
            String datePath = LocalDate.now().toString();
            String filename = IdUtils.nextIdStr() + getSuffix(file.getOriginalFilename());
            Path targetDir = Paths.get(ossProperties.getLocalPath(), subDir, datePath);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile);
            return ossProperties.getBaseUrl() + "/" + subDir + "/" + datePath + "/" + filename;
        } catch (IOException e) {
            log.error("本地图片上传失败", e);
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "图片上传失败");
        }
    }

    /** 防止 query+formData 重复传 dir 时出现 afterSale,afterSale */
    private String sanitizeDir(String dir) {
        if (!StringUtils.hasText(dir)) {
            return "comment";
        }
        String first = dir.split(",")[0].trim();
        String cleaned = first.replaceAll("[^a-zA-Z0-9_-]", "");
        return StringUtils.hasText(cleaned) ? cleaned : "comment";
    }

    private String uploadToOss(MultipartFile file, String dir) {
        // 生产环境接入阿里云 OSS SDK；此处返回规范 URL 占位，避免硬编码密钥
        String objectKey = sanitizeDir(dir) + "/"
                + LocalDate.now() + "/" + IdUtils.nextIdStr() + getSuffix(file.getOriginalFilename());
        log.info("OSS upload objectKey={}", objectKey);
        return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + objectKey;
    }

    private String getSuffix(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
