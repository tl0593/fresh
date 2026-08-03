package com.fresh.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.order")
public class OrderProperties {

    /** 未支付超时秒数，默认 30 分钟 */
    private long unpaidTimeout = 1800;
    /** 拼团过期秒数 */
    private long groupExpireSecond = 86400;
    /** RocketMQ 延时级别（16=30分钟） */
    private int unpaidDelayLevel = 16;

    /** 微信支付 mock 模式 */
    private boolean wxPayMock = true;
    private String wxAppId = "";
    private String wxMchId = "";
    private String wxApiKey = "";
    private String wxNotifyUrl = "http://127.0.0.1:8083/order/pay/notify";
}
