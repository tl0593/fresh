package com.fresh.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    SUCCESS(200, "success"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "无操作权限"),
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "接口不存在"),
    TOO_MANY_REQUESTS(429, "访问过于频繁，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务器繁忙，请稍后再试"),
    INTERNAL_ERROR(500, "系统异常"),

    STOCK_NOT_ENOUGH(10001, "库存不足"),
    ORDER_CANCELLED(10002, "订单已取消"),
    COUPON_EMPTY(10003, "优惠券已领完"),
    AI_SERVICE_ERROR(20001, "AI服务异常");

    private final Integer code;
    private final String msg;

    ErrorCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
