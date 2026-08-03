package com.fresh.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fresh.ai.config.AiProperties;
import com.fresh.ai.dto.ChatMessageDTO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeClient {

    private final AiProperties aiProperties;
    private volatile RestClient restClient;

    public String chat(List<ChatMessageDTO> messages) {
        validateApiKey();
        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getLlm().getModel());
        body.put("messages", toMessageArray(messages));

        JSONObject response = postChatCompletions(body);
        return extractTextContent(response);
    }

    public String chatWithSystem(String systemPrompt, List<ChatMessageDTO> messages) {
        List<ChatMessageDTO> all = new ArrayList<>();
        all.add(new ChatMessageDTO("system", systemPrompt));
        all.addAll(messages);
        return chat(all);
    }

    public String visionAnalyze(String imageUrl, String prompt) {
        validateApiKey();
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "图片地址不能为空");
        }

        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        JSONObject imageUrlObj = new JSONObject();
        imageUrlObj.put("url", imageUrl);
        imagePart.put("image_url", imageUrlObj);

        JSONArray content = new JSONArray();
        content.add(textPart);
        content.add(imagePart);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", content);

        JSONArray messages = new JSONArray();
        messages.add(userMessage);

        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getLlm().getVisionModel());
        body.put("messages", messages);

        JSONObject response = postChatCompletions(body);
        return extractTextContent(response);
    }

    private JSONObject postChatCompletions(JSONObject body) {
        try {
            String responseBody = client().post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getLlm().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toJSONString())
                    .retrieve()
                    .body(String.class);
            return JSON.parseObject(responseBody);
        } catch (RestClientException e) {
            log.error("DashScope API call failed", e);
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR.getCode(), "AI 服务繁忙，请稍后再试");
        }
    }

    private String extractTextContent(JSONObject response) {
        if (response == null) {
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR);
        }
        if (response.containsKey("error")) {
            JSONObject error = response.getJSONObject("error");
            String msg = error != null ? error.getString("message") : "AI 服务异常";
            log.error("DashScope error: {}", response);
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR.getCode(), msg);
        }
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR);
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        if (message == null) {
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR);
        }
        return message.getString("content");
    }

    private JSONArray toMessageArray(List<ChatMessageDTO> messages) {
        JSONArray array = new JSONArray();
        for (ChatMessageDTO msg : messages) {
            JSONObject item = new JSONObject();
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            array.add(item);
        }
        return array;
    }

    private RestClient client() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofMillis(aiProperties.getLlm().getTimeout()));
                    factory.setReadTimeout(Duration.ofMillis(aiProperties.getLlm().getTimeout()));
                    restClient = RestClient.builder()
                            .baseUrl(aiProperties.getLlm().getBaseUrl())
                            .requestFactory(factory)
                            .build();
                }
            }
        }
        return restClient;
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(aiProperties.getLlm().getApiKey())
                || "sk-xxx".equals(aiProperties.getLlm().getApiKey())) {
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR.getCode(), "请配置阿里云百炼 API Key");
        }
    }
}
