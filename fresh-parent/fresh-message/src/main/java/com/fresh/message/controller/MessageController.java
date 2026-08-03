package com.fresh.message.controller;

import com.fresh.common.base.Result;
import com.fresh.message.entity.MsgTemplate;
import com.fresh.message.entity.UserInnerMsg;
import com.fresh.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/inner/list")
    public Result<List<UserInnerMsg>> innerList() {
        return Result.success(messageService.listInnerMsg());
    }

    @PutMapping("/inner/read/{msgId}")
    public Result<Void> innerRead(@PathVariable Long msgId) {
        messageService.readInnerMsg(msgId);
        return Result.success();
    }

    @GetMapping("/template/list")
    public Result<List<MsgTemplate>> templateList() {
        return Result.success(messageService.listTemplate());
    }
}
