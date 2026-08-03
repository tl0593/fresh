package com.fresh.ai.service;

import cn.hutool.core.util.IdUtil;
import com.fresh.ai.client.DashScopeClient;
import com.fresh.ai.dto.ChatMessageDTO;
import com.fresh.ai.dto.ChatSendDTO;
import com.fresh.ai.entity.AiChatRecord;
import com.fresh.ai.mapper.AiChatRecordMapper;
import com.fresh.ai.vo.ChatReplyVO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String CUSTOMER_SYSTEM_PROMPT = """
            你是社区生鲜电商平台的AI智能客服，回答要简洁友好。
            涉及配送、售后、团购等问题时，优先给出可执行建议。
            """;

    private final DashScopeClient dashScopeClient;
    private final AiSessionService sessionService;
    private final AiKnowledgeService knowledgeService;
    private final AiChatRecordMapper chatRecordMapper;
    private final AiMqProducer mqProducer;

    public ChatReplyVO chat(ChatSendDTO dto) {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(dto.getUserMsg())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST);
        }

        String sessionKey = StringUtils.hasText(dto.getSessionKey())
                ? dto.getSessionKey()
                : IdUtil.fastSimpleUUID();

        String aiReply = knowledgeService.matchKnowledge(dto.getUserMsg());
        if (aiReply == null) {
            aiReply = callLlm(userId, sessionKey, dto.getUserMsg(), dto.getChatType());
        } else {
            sessionService.appendMessage(userId, sessionKey, dto.getUserMsg(), aiReply);
        }

        AiChatRecord record = saveRecord(userId, sessionKey, dto, aiReply);
        mqProducer.sendChatBehavior(userId, sessionKey, dto.getChatType());

        ChatReplyVO vo = new ChatReplyVO();
        vo.setRecordId(record.getId());
        vo.setSessionKey(sessionKey);
        vo.setAiReply(aiReply);
        return vo;
    }

    private String callLlm(Long userId, String sessionKey, String userMsg, Integer chatType) {
        List<ChatMessageDTO> history = sessionService.loadHistory(userId, sessionKey);
        history.add(new ChatMessageDTO("user", userMsg));
        try {
            String systemPrompt = buildSystemPrompt(chatType);
            String reply = dashScopeClient.chatWithSystem(systemPrompt, history);
            sessionService.appendMessage(userId, sessionKey, userMsg, reply);
            return reply;
        } catch (BusinessException e) {
            log.warn("LLM fallback for user {}: {}", userId, e.getMessage());
            return fallbackReply(userMsg, chatType);
        }
    }

    private String buildSystemPrompt(Integer chatType) {
        int type = chatType == null ? 1 : chatType;
        return switch (type) {
            case 2 -> CUSTOMER_SYSTEM_PROMPT + "当前场景：为用户推荐个性化菜谱和采购清单。";
            case 3 -> CUSTOMER_SYSTEM_PROMPT + "当前场景：售后咨询，引导用户上传图片并说明处理流程。";
            default -> CUSTOMER_SYSTEM_PROMPT;
        };
    }

    private String fallbackReply(String userMsg, Integer chatType) {
        int type = chatType == null ? 1 : chatType;
        return switch (type) {
            case 2 -> "根据您的偏好，推荐今日菜单：清炒时蔬、番茄牛腩、杂粮饭。请稍后再试获取更详细菜谱。";
            case 3 -> "已收到您的售后咨询，请上传商品照片以便快速处理。";
            default -> "您好，关于「" + userMsg + "」，社区生鲜支持当日达配送，如有问题可随时联系客服。";
        };
    }

    private AiChatRecord saveRecord(Long userId, String sessionKey, ChatSendDTO dto, String aiReply) {
        AiChatRecord record = new AiChatRecord();
        record.setUserId(userId);
        record.setSessionKey(sessionKey);
        record.setUserMsg(dto.getUserMsg());
        record.setAiReply(aiReply);
        record.setChatType(dto.getChatType() == null ? 1 : dto.getChatType());
        record.setCreateTime(LocalDateTime.now());
        chatRecordMapper.insert(record);
        return record;
    }
}
