/** Fresh 后端订单状态（与 OrderConstant 一致）
 * OrderStatus / OrderStatusDesc 定义在主包 services，避免主包 require 分包失败
 */
export { OrderStatus, OrderStatusDesc } from '../../services/order/constants';

// 售后状态 10:待审核,20:已审核,30:已收货,40:收货异常,50:已完成,60:已关闭;
export const AfterServiceStatus = {
  TO_AUDIT: 10,
  THE_APPROVED: 20,
  HAVE_THE_GOODS: 30,
  ABNORMAL_RECEIVING: 40,
  COMPLETE: 50,
  CLOSED: 60,
};

export const ServiceType = {
  RETURN_GOODS: 10,
  ONLY_REFUND: 20,
  ORDER_CANCEL: 30,
};

export const ServiceTypeDesc = {
  [ServiceType.RETURN_GOODS]: '退货',
  [ServiceType.ONLY_REFUND]: '仅退款',
  [ServiceType.ORDER_CANCEL]: '支付后取消',
};

export const OrderButtonTypes = {
  PAY: 1,
  CANCEL: 2,
  CONFIRM: 3,
  APPLY_REFUND: 4,
  VIEW_REFUND: 5,
  COMMENT: 6,
  DELETE: 7,
  DELIVERY: 8,
  REBUY: 9,
  INVITE_GROUPON: 11,
};

export const ServiceButtonTypes = {
  REVOKE: 2,
  FILL_TRACKING_NO: 3,
  CHANGE_TRACKING_NO: 4,
  VIEW_DELIVERY: 5,
};

export const ServiceStatus = {
  PENDING_VERIFY: 100,
  VERIFIED: 110,
  PENDING_DELIVERY: 120,
  PENDING_RECEIPT: 130,
  RECEIVED: 140,
  EXCEPTION: 150,
  REFUNDED: 160,
  CLOSED: 170,
};

export const ServiceReceiptStatus = {
  RECEIPTED: 1,
  NOT_RECEIPTED: 2,
};

export const LogisticsNodeTypes = {
  SUBMITTED: 200001,
  PAYMENTED: 200002,
  SHIPPED: 200003,
  CANCELED: 200004,
  RECEIVED: 200005,
  ADDRESS_CHANGED: 200006,
  IN_TRANSIT: 200007,
};

export const LogisticsIconMap = {
  [LogisticsNodeTypes.SUBMITTED]: '',
  [LogisticsNodeTypes.PAYMENTED]: 'credit_card',
  [LogisticsNodeTypes.SHIPPED]: 'deliver',
  [LogisticsNodeTypes.CANCELED]: '',
  [LogisticsNodeTypes.RECEIVED]: 'check',
  [LogisticsNodeTypes.ADDRESS_CHANGED]: '',
  [LogisticsNodeTypes.IN_TRANSIT]: 'yunshuzhong',
};
