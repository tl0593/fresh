package com.fresh.message.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fresh.common.base.Result;
import com.fresh.message.vo.FeignOrderVO;
import com.fresh.message.vo.FeignUserVO;
import com.fresh.common.util.RedisUtils;
import com.fresh.message.entity.MsgSendLog;
import com.fresh.message.entity.UserInnerMsg;
import com.fresh.message.feign.OrderFeignClient;
import com.fresh.message.feign.UserFeignClient;
import com.fresh.message.mapper.MsgSendLogMapper;
import com.fresh.message.mapper.UserInnerMsgMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushService {

    private static final String DAILY_LIMIT_KEY = "msg:user:daily:";

    private final UserInnerMsgMapper innerMsgMapper;
    private final MsgSendLogMapper sendLogMapper;
    private final UserFeignClient userFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final RedisUtils redisUtils;

    public void handleOrderCreate(String payload) {
        pushOrderMessage("订单创建成功", payload, 1);
    }

    public void handleOrderSuccess(String payload) {
        pushOrderMessage("支付成功，待自提", payload, 2);
    }

    public void handleOrderUnpaid(String payload) {
        pushOrderMessage("订单超时已取消", payload, 3);
    }

    public void handleIntegralChange(String payload) {
        JSONObject json = JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        Long userId = json.getLong("userId");
        if (!checkDailyLimit(userId)) {
            return;
        }
        Integer changeNum = json.getInteger("changeNum");
        String title = "积分变动通知";
        String content = "您的积分变动：" + changeNum + "，" + json.getString("remark");
        saveInnerMsg(userId, title, content);
        saveSendLog(userId, 4, null, content, 1, null);
    }

    public void handleCouponReceive(String payload) {
        JSONObject json = JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        Long userId = json.getLong("userId");
        if (!checkDailyLimit(userId)) {
            return;
        }
        String content = "恭喜您成功领取优惠券，请在有效期内使用";
        saveInnerMsg(userId, "优惠券到账", content);
        saveSendLog(userId, 5, json.getLong("userCouponId"), content, 1, null);
    }

    public void handleCommentAdd(String payload) {
        JSONObject json = JSON.parseObject(payload);
        if (json == null) {
            return;
        }
        String content = "用户提交了新的商品评价，商品ID：" + json.getLong("goodsId");
        saveInnerMsg(0L, "新评价提醒", content);
        saveSendLog(json.getLong("userId"), 6, json.getLong("commentId"), content, 1, null);
    }

    public void handleAiRecognizeFinish(String payload) {
        JSONObject json = JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        Long userId = json.getLong("userId");
        String content = "您的售后图片 AI 识别已完成，请查看理赔建议";
        saveInnerMsg(userId, "售后识别完成", content);
        saveSendLog(userId, 7, json.getLong("afterSaleId"), content, 1, null);
    }

    private void pushOrderMessage(String title, String payload, int businessType) {
        JSONObject json = JSON.parseObject(payload);
        if (json == null || json.getLong("userId") == null) {
            return;
        }
        Long userId = json.getLong("userId");
        if (!checkDailyLimit(userId)) {
            return;
        }
        String orderNo = json.getString("orderNo");
        String content = title + "，订单号：" + orderNo;
        if (orderNo != null) {
            try {
                Result<FeignOrderVO> orderResult = orderFeignClient.getOrderByNo(orderNo);
                if (orderResult.getData() != null) {
                    content = content + "，金额：" + orderResult.getData().getPayAmount();
                }
            } catch (Exception e) {
                log.debug("Feign 查询订单失败 orderNo={}", orderNo);
            }
        }
        saveInnerMsg(userId, title, content);
        String openid = fetchOpenid(userId);
        saveSendLog(userId, businessType, json.getLong("orderId"), content, 1, openid);
    }

    private String fetchOpenid(Long userId) {
        try {
            Result<FeignUserVO> userResult = userFeignClient.getUserById(userId);
            if (userResult.getData() != null) {
                return userResult.getData().getOpenid();
            }
        } catch (Exception e) {
            log.debug("Feign 查询用户 openid 失败 userId={}", userId);
        }
        return null;
    }

    private boolean checkDailyLimit(Long userId) {
        String key = DAILY_LIMIT_KEY + userId + ":" + LocalDate.now();
        Long count = redisUtils.increment(key);
        if (count == 1) {
            redisUtils.expire(key, 1, TimeUnit.DAYS);
        }
        return count <= 50;
    }

    private void saveInnerMsg(Long userId, String title, String content) {
        UserInnerMsg msg = new UserInnerMsg();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setReadFlag(0);
        msg.setCreateTime(LocalDateTime.now());
        innerMsgMapper.insert(msg);
    }

    private void saveSendLog(Long userId, int businessType, Long businessId,
                             String content, int status, String openid) {
        MsgSendLog log = new MsgSendLog();
        log.setUserId(userId);
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setSendContent(content);
        log.setOpenid(openid);
        log.setSendStatus(status);
        log.setCreateTime(LocalDateTime.now());
        sendLogMapper.insert(log);
    }
}
