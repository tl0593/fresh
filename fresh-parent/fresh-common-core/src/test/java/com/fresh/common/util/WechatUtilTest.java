package com.fresh.common.util;

import com.fresh.common.config.WechatProperties;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {WechatUtil.class, WechatProperties.class})
@TestPropertySource(properties = {
        "fresh.wechat.mock-enabled=true",
        "fresh.wechat.app-id="
})
class WechatUtilTest {

    @Autowired
    private WechatUtil wechatUtil;

    @Test
    void code2Session_mockMode_returnsWxPrefix() {
        String openid = wechatUtil.code2Session("test-code-001");
        assertThat(openid).isEqualTo("wx_test-code-001");
    }
}
