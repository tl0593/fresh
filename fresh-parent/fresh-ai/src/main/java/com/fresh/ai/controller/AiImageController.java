package com.fresh.ai.controller;

import com.fresh.ai.dto.AfterSaleImageMqDTO;
import com.fresh.ai.dto.ImageDamageResultDTO;
import com.fresh.ai.service.AiImageRecognizeService;
import com.fresh.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/image")
@RequiredArgsConstructor
public class AiImageController {

    private final AiImageRecognizeService imageRecognizeService;

    /** 本地测试：同步触发完整售后识别流程（无需 MQ） */
    @PostMapping("/rec/test")
    public Result<ImageDamageResultDTO> recognizeTest(@RequestBody AfterSaleImageMqDTO dto) {
        return Result.success(imageRecognizeService.handleAfterSaleImage(dto));
    }
}
