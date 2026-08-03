package com.fresh.goods.service;

import com.fresh.goods.config.OssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OssUploadServiceTest {

    @TempDir
    Path tempDir;

    private OssUploadService ossUploadService;

    @BeforeEach
    void setUp() {
        OssProperties props = new OssProperties();
        props.setEnabled(false);
        props.setLocalPath(tempDir.toString());
        props.setBaseUrl("http://localhost/upload");
        ossUploadService = new OssUploadService(props);
    }

    @Test
    void uploadImage_localMode_returnsUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image".getBytes());
        String url = ossUploadService.uploadImage(file, "comment");
        assertThat(url).startsWith("http://localhost/upload/comment/");
        assertThat(url).endsWith(".jpg");
    }
}
