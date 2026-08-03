package com.fresh.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FreshUserApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.stream.function.definition="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String miniToken;
    private static Long miniUserId;

    @Test
    @Order(1)
    void miniLogin_createsUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/mini/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"junit-mini-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.nickName").isNotEmpty());

        MvcResult result = mockMvc.perform(post("/mini/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"junit-mini-001\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        miniToken = data.path("token").asText();
        miniUserId = data.path("userInfo").path("id").asLong();
        assertThat(miniToken).isNotBlank();
        assertThat(miniUserId).isPositive();
        // mock 模式下 openid = wx_{code}
        assertThat(data.path("userInfo").path("openid").asText()).startsWith("wx_");
    }

    @Test
    @Order(2)
    void miniLogin_rejectsEmptyCode() throws Exception {
        mockMvc.perform(post("/mini/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(3)
    void adminLogin_withSeedAccount() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @Order(4)
    void adminLogin_rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(5)
    void cart_requiresAuthorization() throws Exception {
        mockMvc.perform(get("/cart/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @Order(6)
    void cart_updateAndList() throws Exception {
        mockMvc.perform(post("/cart/update")
                        .header("Authorization", miniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goodsId\":1,\"specId\":1,\"num\":3,\"selected\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/cart/list").header("Authorization", miniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].goodsId").value(1))
                .andExpect(jsonPath("$.data[0].num").value(3));
    }

    @Test
    @Order(7)
    void address_saveAndList() throws Exception {
        mockMvc.perform(post("/address/save")
                        .header("Authorization", miniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"集成测试\",\"phone\":\"13800001111\",\"community\":\"测试社区\",\"detailAddr\":\"A座101\",\"isDefault\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/address/list").header("Authorization", miniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].phone").value("13800001111"));
    }

    @Test
    @Order(8)
    void feign_getUserById() throws Exception {
        mockMvc.perform(get("/feign/user/" + miniUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(miniUserId));
    }

    @Test
    @Order(9)
    void feign_dailyStat() throws Exception {
        mockMvc.perform(get("/feign/user/dailyStat").param("statDate", "2026-07-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.newUser").isNumber());
    }

    @Test
    @Order(10)
    void integral_freezeUnfreezeAndChange() throws Exception {
        MvcResult beforeResult = mockMvc.perform(get("/feign/user/" + miniUserId))
                .andExpect(status().isOk())
                .andReturn();
        int baseIntegral = objectMapper.readTree(beforeResult.getResponse().getContentAsString())
                .path("data").path("integral").asInt();

        mockMvc.perform(post("/feign/integral/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + miniUserId + ",\"integral\":100,\"orderId\":9001,\"remark\":\"测试充值\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/feign/integral/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + miniUserId + ",\"integral\":30,\"orderId\":9001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/feign/user/" + miniUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integral").value(baseIntegral + 70));

        mockMvc.perform(post("/feign/integral/unfreeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + miniUserId + ",\"integral\":30,\"orderId\":9001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/feign/user/" + miniUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integral").value(baseIntegral + 100));

        mockMvc.perform(get("/integral/log").header("Authorization", miniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].changeNum").value(100));
    }

    @Test
    @Order(11)
    void integral_freezeRejectsInsufficientBalance() throws Exception {
        mockMvc.perform(post("/feign/integral/freeze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + miniUserId + ",\"integral\":999999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("积分不足"));
    }
}
