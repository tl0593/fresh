package com.fresh.ai.controller;

import com.fresh.ai.entity.AiChatRecord;
import com.fresh.ai.entity.AiGroupText;
import com.fresh.ai.entity.AiImageRecognize;
import com.fresh.ai.service.AiLogService;
import com.fresh.common.base.PageVO;
import com.fresh.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAdminController {

    private final AiLogService logService;

    @GetMapping("/chat/log/page")
    public Result<PageVO<AiChatRecord>> chatLogPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(logService.chatLogPage(pageNum, pageSize, userId));
    }

    @GetMapping("/image/rec/log/page")
    public Result<PageVO<AiImageRecognize>> imageRecLogPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "afterSaleId", required = false) Long afterSaleId) {
        return Result.success(logService.imageRecLogPage(pageNum, pageSize, afterSaleId));
    }

    @GetMapping("/group/text/log/page")
    public Result<PageVO<AiGroupText>> groupTextLogPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "goodsId", required = false) Long goodsId) {
        return Result.success(logService.groupTextLogPage(pageNum, pageSize, goodsId));
    }
}
