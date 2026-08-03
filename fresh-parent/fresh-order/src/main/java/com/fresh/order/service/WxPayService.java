package com.fresh.order.service;

import com.fresh.common.constant.OrderConstant;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.IdUtils;
import com.fresh.order.config.OrderProperties;
import com.fresh.order.entity.OrderMain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxPayService {

    private final OrderProperties orderProperties;
    private final OrderService orderService;

    /**
     * 创建微信预支付单；mock 模式返回模拟 prepay 参数供小程序调起支付。
     */
    public Map<String, String> createPrepay(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "缺少订单号");
        }
        OrderMain order = orderService.getByOrderNo(orderNo);
        Long userId = ContextUtil.getUserId();
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        if (order.getStatus() != OrderConstant.STATUS_UNPAID) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "订单状态不可支付");
        }
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("payAmount", order.getPayAmount().toPlainString());
        if (orderProperties.isWxPayMock()) {
            result.put("mock", "true");
            result.put("prepayId", "mock_prepay_" + IdUtils.nextIdStr());
            result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            result.put("nonceStr", IdUtils.nextIdStr());
            result.put("package", "prepay_id=" + result.get("prepayId"));
            result.put("signType", "RSA");
            result.put("paySign", "MOCK_SIGN");
            log.info("mock 预支付 orderNo={}, amount={}", orderNo, order.getPayAmount());
        } else {
            // P3：对接微信 APIv3 统一下单后在此返回真实 timeStamp/nonceStr/package/paySign
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(),
                    "真实微信支付尚未对接，请保持 fresh.order.wx-pay-mock=true");
        }
        return result;
    }

    /**
     * 开发/mock 回调：仅 wx-pay-mock=true 时可用。
     */
    public void handleMockCallback(Map<String, String> params) {
        if (!orderProperties.isWxPayMock()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(),
                    "非 mock 模式禁止使用 /pay/callback，请走微信异步通知");
        }
        handleNotify(params);
    }

    /**
     * 处理微信支付回调（mock 模式仅校验 orderNo）。
     */
    public void handleNotify(Map<String, String> params) {
        String orderNo = params.get("orderNo");
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "缺少订单号");
        }
        if (!orderProperties.isWxPayMock()) {
            // P3：生产环境在此校验微信 APIv3 签名并解密 resource
            String sign = params.get("sign");
            if (sign == null || sign.isBlank()) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "支付签名校验失败");
            }
        }
        log.info("支付回调 orderNo={}, mock={}", orderNo, orderProperties.isWxPayMock());
        orderService.paySuccess(orderNo, params.get("transactionId"), params);
    }
}
