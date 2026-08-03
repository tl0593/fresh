package com.fresh.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fresh.common.config.WechatProperties;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUtil {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final WechatProperties wechatProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 微信 code 换取 openid；mock 模式或缺少 appId 时返回 wx_{code}。
     */
    public String code2Session(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "微信 code 不能为空");
        }
        if (wechatProperties.isMockEnabled() || !StringUtils.hasText(wechatProperties.getAppId())) {
            return "wx_" + code;
        }
        try {
            String url = String.format(CODE2SESSION_URL,
                    wechatProperties.getAppId(), wechatProperties.getAppSecret(), code);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = JSON.parseObject(response.body());
            if (json.containsKey("errcode") && json.getIntValue("errcode") != 0) {
                log.warn("微信 code2session 失败: {}", response.body());
                throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "微信授权失败");
            }
            String openid = json.getString("openid");
            if (!StringUtils.hasText(openid)) {
                throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "微信授权未返回 openid");
            }
            return openid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "微信授权接口调用失败");
        }
    }
}
