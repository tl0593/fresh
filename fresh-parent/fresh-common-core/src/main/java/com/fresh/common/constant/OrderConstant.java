package com.fresh.common.constant;

public final class OrderConstant {

    private OrderConstant() {
    }

    /** 待支付 */
    public static final int STATUS_UNPAID = 0;
    /** 待自提（已到自提点） */
    public static final int STATUS_WAIT_PICKUP = 1;
    /** 已完成 */
    public static final int STATUS_COMPLETED = 2;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 3;
    /** 售后中 */
    public static final int STATUS_AFTER_SALE = 4;
    /** 待配送（已支付，配送至自提点途中） */
    public static final int STATUS_WAIT_DELIVERY = 5;

    public static final int ACTIVITY_NORMAL = 1;
    public static final int ACTIVITY_GROUP = 2;
    public static final int ACTIVITY_SECKILL = 3;
}
