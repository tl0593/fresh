import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { yuanToFen } from '../adapters/goods';
import { OrderStatus, OrderStatusDesc } from './constants';
import { isItemCommented } from './orderSubmitComment';

/** 获取订单详情mock数据 */
function mockFetchOrderDetail(params) {
  const { delay } = require('../_utils/delay');
  const { genOrderDetail } = require('../../model/order/orderDetail');

  return delay().then(() => genOrderDetail(params));
}

function resolveOrderNo(params) {
  if (!params) return '';
  if (typeof params.parameter === 'string') return params.parameter;
  return params.parameter?.orderNo || params.orderNo || '';
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
  }
  if (status === OrderStatus.AFTER_SALE) {
    buttons.push({ primary: false, type: 5, name: '查看售后' });
  }
  return buttons;
}

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

/** 获取订单详情数据 */
export async function fetchOrderDetail(params) {
  if (!config.useFreshPay && config.useMock) {
    return mockFetchOrderDetail(params);
  }

  await ensureLogin();
  const orderNo = resolveOrderNo(params);
  const detail = await request.get(`/api/order/order/${orderNo}`);
  const order = detail.order || detail;
  let items = detail.items || [];
  const payTime = order.payTime || null;
  const status = order.status;
  if (status === OrderStatus.COMPLETE) {
    items = await mergeDoneCommentFlags(order.orderNo || orderNo, items);
  } else {
    items = items.map((it) => ({ ...it, isCommented: isItemCommented(it) ? 1 : 0 }));
  }

  return {
    data: {
      orderId: order.id,
      orderNo: order.orderNo,
      parentOrderNo: order.orderNo,
      storeId: '1',
      storeName: order.community || '社区自提点',
      orderStatus: status,
      orderStatusName: OrderStatusDesc[status] || '未知',
      orderSubStatus: 0,
      paymentAmount: yuanToFen(order.payAmount),
      totalAmount: yuanToFen(order.totalAmount),
      goodsAmountApp: yuanToFen(order.totalAmount),
      createTime: order.createTime,
      autoCancelTime: null,
      invoiceStatus: 3,
      invoiceDesc: '不开发票',
      invoiceVO: null,
      paymentVO: {
        paySuccessTime: payTime,
      },
      logisticsVO: {
        logisticsNo: '',
        receiverName: order.receiverName,
        receiverPhone: order.receiverPhone,
        receiverAddress: `${order.community || ''}${order.detailAddress || ''}`,
        receiverProvince: '',
        receiverCity: '',
        receiverCountry: '',
        receiverArea: '',
      },
      receiverAddress: `${order.community || ''}${order.detailAddress || ''}`,
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
      trajectoryVos: [],
      groupInfoVo: null,
    },
    code: 'Success',
    success: true,
  };
}

/** 获取客服mock数据 */
function mockFetchBusinessTime(params) {
  const { delay } = require('../_utils/delay');
  const { genBusinessTime } = require('../../model/order/orderDetail');

  return delay().then(() => genBusinessTime(params));
}

/** 获取客服数据 */
export function fetchBusinessTime(params) {
  if (!config.useFreshPay && config.useMock) {
    return mockFetchBusinessTime(params);
  }

  return Promise.resolve({
    data: {
      telphone: '',
      businessTime: ['自提点营业时间以门店公告为准'],
    },
    code: 'Success',
    success: true,
  });
}
