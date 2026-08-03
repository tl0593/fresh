package com.fresh.goods.controller;

import com.fresh.goods.config.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 显式提供本地上传文件下载，避免 ResourceHandler 找不到文件时抛成 500 JSON，
 * 导致小程序把「系统异常」当图片解析失败。
 */
@RestController
@RequiredArgsConstructor
public class LocalFileController {

    private final OssProperties ossProperties;

    @GetMapping("/upload/**")
    public ResponseEntity<Resource> getUploadFile(HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (!StringUtils.hasText(fullPath) || !fullPath.startsWith("/upload/")) {
            return ResponseEntity.notFound().build();
        }
        String relative = fullPath.substring("/upload/".length());
        // 防目录穿越
        if (relative.contains("..") || relative.startsWith("/") || relative.startsWith("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Path root = Paths.get(ossProperties.getLocalPath()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return ResponseEntity.notFound().build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(target);
        } catch (Exception e) {
            contentType = null;
        }
        if (!StringUtils.hasText(contentType)) {
            contentType = guessContentType(target.getFileName().toString());
        }

        FileSystemResource body = new FileSystemResource(target);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    private String guessContentType(String name) {
        String lower = name == null ? "" : name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
