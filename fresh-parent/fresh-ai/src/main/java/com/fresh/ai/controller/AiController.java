package com.fresh.ai.controller;

import com.fresh.ai.dto.ChatSendDTO;
import com.fresh.ai.dto.GroupTextGenerateDTO;
import com.fresh.ai.entity.AiKnowledge;
import com.fresh.ai.service.AiChatService;
import com.fresh.ai.service.AiCookService;
import com.fresh.ai.service.AiGroupTextService;
import com.fresh.ai.service.AiKnowledgeService;
import com.fresh.ai.vo.ChatReplyVO;
import com.fresh.ai.vo.CookHistoryVO;
import com.fresh.ai.vo.GroupTextVO;
import com.fresh.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AiController {

    private final AiChatService chatService;
    private final AiCookService cookService;
    private final AiGroupTextService groupTextService;
    private final AiKnowledgeService knowledgeService;

    @PostMapping("/ai/chat/send")
    public Result<ChatReplyVO> chatSend(@RequestBody ChatSendDTO dto) {
        return Result.success(chatService.chat(dto));
    }

    @GetMapping("/ai/cook/generate")
    public Result<String> cookGenerate(@RequestParam(value = "preference", required = false) String preference) {
        return Result.success(cookService.generateCook(preference));
    }

    @GetMapping("/ai/cook/history")
    public Result<CookHistoryVO> cookHistory() {
        return Result.success(cookService.getCookHistory());
    }

    @PostMapping("/ai/group/text/generate")
    public Result<GroupTextVO> groupTextGenerate(@RequestBody GroupTextGenerateDTO dto) {
        return Result.success(groupTextService.generate(dto));
    }

    @GetMapping("/ai/knowledge/list")
    public Result<List<AiKnowledge>> knowledgeList() {
        return Result.success(knowledgeService.listKnowledge());
    }

    @GetMapping("/ai/knowledge/{id}")
    public Result<AiKnowledge> knowledgeDetail(@PathVariable Long id) {
        return Result.success(knowledgeService.getKnowledge(id));
    }

    @PostMapping("/ai/knowledge/save")
    public Result<Void> knowledgeSave(@RequestBody AiKnowledge knowledge) {
        knowledgeService.saveKnowledge(knowledge);
        return Result.success();
    }

    @PutMapping("/ai/knowledge/update")
    public Result<Void> knowledgeUpdate(@RequestBody AiKnowledge knowledge) {
        knowledgeService.updateKnowledge(knowledge);
        return Result.success();
    }

    @DeleteMapping("/ai/knowledge/{id}")
    public Result<Void> knowledgeDelete(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.success();
    }
}
