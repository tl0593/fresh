package com.fresh.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.wechat")
public class WechatProperties {

    /** 小程序 AppId，留空则走 mock 模式 */
    private String appId = "";
    /** 小程序 AppSecret */
    private String appSecret = "";
    /** true=始终 mock openid（本地开发） */
    private boolean mockEnabled = true;
}
