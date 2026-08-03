package com.fresh.goods.controller;

import com.fresh.common.base.Result;
import com.fresh.goods.service.OssUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final OssUploadService ossUploadService;

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "dir", defaultValue = "comment") String dir) {
        String url = ossUploadService.uploadImage(file, dir);
        return Result.success(Map.of("url", url));
    }
}
