/** Fresh 订单状态常量（放在主包，供 services 与分包共用） */
export const OrderStatus = {
  PENDING_PAYMENT: 0,
  WAIT_PICKUP: 1,
  COMPLETE: 2,
  CANCELED: 3,
  AFTER_SALE: 4,
  /** 待配送（已支付，配送至自提点） */
  PENDING_DELIVERY: 5,
  PENDING_RECEIPT: 1,
  PAYMENT_TIMEOUT: 3,
  CANCELED_NOT_PAYMENT: 3,
  CANCELED_PAYMENT: 3,
  CANCELED_REJECTION: 3,
};

export const OrderStatusDesc = {
  [OrderStatus.PENDING_PAYMENT]: '待支付',
  [OrderStatus.PENDING_DELIVERY]: '待配送',
  [OrderStatus.WAIT_PICKUP]: '待自提',
  [OrderStatus.COMPLETE]: '已完成',
  [OrderStatus.CANCELED]: '已取消',
  [OrderStatus.AFTER_SALE]: '售后中',
};
