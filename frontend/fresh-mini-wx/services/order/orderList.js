import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { yuanToFen } from '../adapters/goods';
import { OrderStatus, OrderStatusDesc } from './constants';
import { isItemCommented } from './orderSubmitComment';

/** 获取订单列表mock数据 */
function mockFetchOrders(params) {
  const { delay } = require('../_utils/delay');
  const { genOrders } = require('../../model/order/orderList');

  return delay(200).then(() => genOrders(params));
}

function buildButtons(order, items) {
  const status = order.status;
  const buttons = [];
  if (status === OrderStatus.PENDING_PAYMENT) {
    buttons.push({ primary: true, type: 1, name: '付款' });
  }
  if (status === OrderStatus.WAIT_PICKUP || status === OrderStatus.PENDING_DELIVERY || status === OrderStatus.COMPLETE) {
    buttons.push({ primary: false, type: 4, name: '申请售后' });
  }
  if (status === OrderStatus.COMPLETE) {
    const canComment = (items || []).some((it) => !isItemCommented(it));
    if (canComment) {
      buttons.push({ primary: true, type: 6, name: '评价' });
    }
    buttons.push({ primary: false, type: 9, name: '再来一单' });
  }
  if (status === OrderStatus.AFTER_SALE) {
    buttons.push({ primary: false, type: 5, name: '查看售后' });
  }
  return buttons;
}

/** 用评价表纠正订单项 isCommented（历史数据标记可能未同步） */
async function mergeDoneCommentFlags(orderNo, items) {
  const normalized = (items || []).map((it) => ({
    ...it,
    isCommented: isItemCommented(it) ? 1 : 0,
  }));
  if (!orderNo) return normalized;
  try {
    const done = (await request.get(`/api/goods/comment/order/${encodeURIComponent(orderNo)}/done`)) || [];
    const doneSet = new Set((done || []).map((id) => String(id)));
    return normalized.map((it) => ({
      ...it,
      isCommented: it.isCommented || doneSet.has(String(it.id)) ? 1 : 0,
    }));
  } catch (e) {
    return normalized;
  }
}

function mapFreshOrder(order, items) {
  const status = order.status;
  return {
    orderId: order.id,
    orderNo: order.orderNo,
    parentOrderNo: order.orderNo,
    storeId: '1',
    storeName: order.community || '社区自提点',
    orderStatus: status,
    orderStatusName: OrderStatusDesc[status] || '未知',
    paymentAmount: yuanToFen(order.payAmount),
    totalAmount: yuanToFen(order.totalAmount),
    amount: yuanToFen(order.payAmount),
    createTime: order.createTime,
    logisticsVO: { logisticsNo: '' },
    orderItemVOs: items.map((it) => ({
      id: it.id,
      goodsName: it.goodsName,
      goodsPictureUrl: it.goodsImg,
      actualPrice: yuanToFen(it.price),
      tagPrice: yuanToFen(it.price),
      buyQuantity: it.num,
      skuId: it.specId,
      spuId: it.goodsId,
      isCommented: isItemCommented(it) ? 1 : 0,
      specifications: [],
      buttonVOs:
        status === OrderStatus.WAIT_PICKUP ||
        status === OrderStatus.PENDING_DELIVERY ||
        status === OrderStatus.COMPLETE
          ? [{ primary: false, type: 4, name: '申请售后' }]
          : [],
    })),
    buttonVOs: buildButtons(order, items),
  };
}

/** 获取订单列表数据 */
export async function fetchOrders(params) {
  if (!config.useFreshPay && config.useMock) {
    return mockFetchOrders(params);
  }

  await ensureLogin();
  const list = await request.get('/api/order/order/list');
  const statusFilter = params?.parameter?.orderStatus;

  let orders = await Promise.all(
    (list || []).map(async (row) => {
      const order = row.order || row;
      let items = row.items || [];
      if (order.status === OrderStatus.COMPLETE) {
        items = await mergeDoneCommentFlags(order.orderNo, items);
      } else {
        items = (items || []).map((it) => ({
          ...it,
          isCommented: isItemCommented(it) ? 1 : 0,
        }));
      }
      return mapFreshOrder(order, items);
    }),
  );

  // 有售后记录的订单号（含已审核），用于从「已完成」中排除
  let afterSaleOrderNos = new Set();
  try {
    const afterSales = (await request.get('/api/order/afterSale/mine')) || [];
    afterSaleOrderNos = new Set(afterSales.map((a) => a && a.orderNo).filter(Boolean));
  } catch (e) {
    /* 旧后端无此接口时忽略 */
  }

  if (statusFilter !== undefined && statusFilter !== -1 && statusFilter !== null) {
    orders = orders.filter((o) => {
      if (Number(statusFilter) === OrderStatus.COMPLETE) {
        // 已售后订单不出现在「已完成」
        return o.orderStatus === OrderStatus.COMPLETE && !afterSaleOrderNos.has(o.orderNo);
      }
      if (Number(statusFilter) === OrderStatus.AFTER_SALE) {
        return o.orderStatus === OrderStatus.AFTER_SALE || afterSaleOrderNos.has(o.orderNo);
      }
      return o.orderStatus === statusFilter;
    });
  }
  return {
    data: { orders, totalCount: orders.length },
    code: 'Success',
    success: true,
  };
}

/** 获取订单列表mock数据 */
function mockFetchOrdersCount(params) {
  const { delay } = require('../_utils/delay');
  const { genOrdersCount } = require('../../model/order/orderList');

  return delay().then(() => genOrdersCount(params));
}

/** 获取订单列表统计 */
export async function fetchOrdersCount(params) {
  if (!config.useFreshPay && config.useMock) {
    return mockFetchOrdersCount(params);
  }

  await ensureLogin();
  const list = await request.get('/api/order/order/list');
  const counts = [
    { tabType: 0, orderNum: 0 },
    { tabType: 5, orderNum: 0 },
    { tabType: 1, orderNum: 0 },
    { tabType: 2, orderNum: 0 },
    { tabType: 3, orderNum: 0 },
    { tabType: 4, orderNum: 0 },
  ];
  (list || []).forEach((row) => {
    const status = row.order ? row.order.status : row.status;
    const hit = counts.find((c) => c.tabType === status);
    if (hit) hit.orderNum += 1;
  });
  return {
    data: counts,
    code: 'Success',
    success: true,
  };
}
