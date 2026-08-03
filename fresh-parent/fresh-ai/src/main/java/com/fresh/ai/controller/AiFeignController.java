package com.fresh.ai.controller;

import com.fresh.ai.dto.AfterSaleImageMqDTO;
import com.fresh.ai.dto.GoodsInfoDTO;
import com.fresh.ai.dto.ImageDamageResultDTO;
import com.fresh.ai.service.AiCookService;
import com.fresh.ai.service.AiGroupTextService;
import com.fresh.ai.service.AiImageRecognizeService;
import com.fresh.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feign")
@RequiredArgsConstructor
public class AiFeignController {

    private final AiGroupTextService groupTextService;
    private final AiCookService cookService;
    private final AiImageRecognizeService imageRecognizeService;

    @PostMapping("/group/text")
    public Result<String> generateGroupText(@RequestBody GoodsInfoDTO dto) {
        return Result.success(groupTextService.generateForFeign(dto));
    }

    @PostMapping("/cook/batch")
    public Result<List<String>> batchGenerateCook(@RequestBody List<Long> goodsIdList) {
        return Result.success(cookService.batchGenerateCook(goodsIdList));
    }

    @PostMapping("/image/recognize")
    public Result<ImageDamageResultDTO> recognizeImage(@RequestBody AfterSaleImageMqDTO dto) {
        ImageDamageResultDTO result = imageRecognizeService.recognize(dto.getImgUrl());
        return Result.success(result);
    }
}
