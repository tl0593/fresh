import dayjs from 'dayjs';
import { config } from '../../../config/index';
import { mockIp, mockReqId } from '../../../utils/mock';
import { ServiceStatus, ServiceType } from '../config';

export const formatTime = (date, template) => dayjs(date).format(template);

function yuanToFen(yuan) {
  const n = Number(yuan);
  if (Number.isNaN(n)) return 0;
  return Math.round(n * 100);
}

/** 将订单详情适配为售后详情页所需结构 */
export function adaptOrderToRightsDetail(detail, rightsNo) {
  const order = (detail && detail.order) || detail || {};
  const items = (detail && detail.items) || [];
  const orderNo = order.orderNo || rightsNo || '';
  const payFen = yuanToFen(order.payAmount);
  const isAfterSale = Number(order.status) === 4;

  return {
    buttonVOs: [],
    createTime: order.createTime || Date.now(),
    storeId: '1',
    refundMethodList: [],
    rights: {
      bizRightsStatus: 1,
      bizRightsStatusName: isAfterSale ? '售后中' : '已申请',
      createTime: order.createTime || Date.now(),
      orderNo,
      refundAmount: payFen,
      refundRequestAmount: payFen,
      rightsNo: orderNo,
      rightsStatus: 10,
      rightsStatusName: '待审核',
      rightsType: ServiceType.ONLY_REFUND,
      storeName: order.community || '社区自提点',
      userRightsStatus: ServiceStatus.PENDING_VERIFY,
      userRightsStatusName: '待商家审核',
      userRightsStatusDesc: '售后申请已提交，请等待商家审核处理',
      afterSaleRequireType: 'REFUND_MONEY',
      rightsReasonDesc: '',
      rightsImageUrls: [],
    },
    rightsItem: (items.length ? items : [{}]).map((it) => ({
      goodsName: it.goodsName || '生鲜商品',
      goodsPictureUrl: it.goodsImg || '',
      skuId: it.specId,
      spuId: it.goodsId,
      rightsQuantity: it.num || 1,
      itemRefundAmount: yuanToFen(it.subTotal != null ? it.subTotal : Number(it.price || 0) * Number(it.num || 1)),
      refundAmount: yuanToFen(it.subTotal != null ? it.subTotal : Number(it.price || 0) * Number(it.num || 1)),
      specInfo: [],
    })),
    rightsRefund: {
      traceNo: '',
      refundDesc: '',
    },
    logisticsVO: {
      logisticsNo: '',
      logisticsCompanyName: '',
      logisticsCompanyCode: '',
      remark: '',
      receiverName: order.receiverName || '',
      receiverPhone: order.receiverPhone || '',
      receiverProvince: '',
      receiverCity: '',
      receiverCountry: '',
      receiverArea: '',
      receiverAddress: order.receiverAddress || '',
    },
  };
}

function adaptAfterSaleRow(as) {
  const audit = Number(as.auditStatus);
  let userRightsStatusName = '待商家审核';
  let userRightsStatusDesc = '售后申请已提交，请等待商家审核处理';
  let userRightsStatus = ServiceStatus.PENDING_VERIFY;
  if (audit === 1) {
    userRightsStatusName = '退款已处理';
    userRightsStatusDesc = '商家已通过售后，退款将按原路返回';
    userRightsStatus = ServiceStatus.REFUNDED;
  } else if (audit === 2) {
    userRightsStatusName = '售后已驳回';
    userRightsStatusDesc = '商家已驳回本次售后申请';
    userRightsStatus = ServiceStatus.CLOSED;
  }
  const refundFen = yuanToFen(
    as.actualRefundMoney != null ? as.actualRefundMoney : as.aiRefundMoney != null ? as.aiRefundMoney : as.itemPrice,
  );
  // 多图用 |；单条 URL 内可能含历史脏目录 afterSale,afterSale，勿按逗号拆
  const images = as.damageImg
    ? String(as.damageImg)
        .split('|')
        .map((s) => s.trim())
        .filter((s) => s && !/^https?:\/\/tmp\//i.test(s))
    : [];
  const fixedImages = images;

  return {
    buttonVOs: [],
    createTime: as.createTime,
    storeId: '1',
    refundMethodList: [],
    rights: {
      bizRightsStatus: 1,
      bizRightsStatusName: userRightsStatusName,
      createTime: as.createTime,
      orderNo: as.orderNo,
      refundAmount: refundFen,
      refundRequestAmount: refundFen,
      rightsNo: String(as.id),
      rightsStatus: audit === 1 ? 50 : audit === 2 ? 60 : 10,
      rightsStatusName: userRightsStatusName,
      rightsType: ServiceType.ONLY_REFUND,
      storeName: '社区自提点',
      userRightsStatus,
      userRightsStatusName,
      userRightsStatusDesc,
      afterSaleRequireType: 'REFUND_MONEY',
      rightsReasonDesc: as.remark || '',
      rightsImageUrls: fixedImages.length ? fixedImages : images,
    },
    rightsItem: [
      {
        goodsName: as.goodsName || '生鲜商品',
        goodsPictureUrl: as.goodsImg || '',
        rightsQuantity: as.itemNum || 1,
        itemRefundAmount: refundFen,
        refundAmount: refundFen,
        specInfo: [],
      },
    ],
    rightsRefund: { traceNo: '', refundDesc: as.remark || '' },
    logisticsVO: {
      logisticsNo: '',
      logisticsCompanyName: '',
      logisticsCompanyCode: '',
      remark: '',
      receiverName: '',
      receiverPhone: '',
      receiverProvince: '',
      receiverCity: '',
      receiverCountry: '',
      receiverArea: '',
      receiverAddress: '',
    },
  };
}

export async function getRightsDetail({ rightsNo }) {
  if (config.useMock) {
    const resp = require('../after-service-list/mock-data').resp;
    const filtered = (resp.data.dataList || []).filter(
      (item) => item.rights && item.rights.rightsNo === rightsNo,
    );
    return {
      data: filtered,
      code: 'Success',
      msg: null,
      requestId: mockReqId(),
      clientIp: mockIp(),
      success: true,
    };
  }

  if (!rightsNo) {
    return { data: [], success: false, msg: '缺少售后单号' };
  }

  const { ensureLogin } = require('../../../services/auth/login');
  const request = require('../../../utils/request').default;
  await ensureLogin();

  try {
    // rightsNo 优先按售后工单 id；否则按订单号兜底
    const rows = (await request.get('/api/order/afterSale/mine')) || [];
    const byId = rows.find((r) => String(r.id) === String(rightsNo));
    if (byId) {
      return { data: [adaptAfterSaleRow(byId)], code: 'Success', success: true };
    }
    const byOrder = rows.find((r) => String(r.orderNo) === String(rightsNo));
    if (byOrder) {
      return { data: [adaptAfterSaleRow(byOrder)], code: 'Success', success: true };
    }
    const detail = await request.get(`/api/order/order/${rightsNo}`);
    if (!detail || (!(detail.order || detail.orderNo) && !(detail.items || []).length)) {
      return { data: [], success: false, msg: '售后详情不存在' };
    }
    return {
      data: [adaptOrderToRightsDetail(detail, rightsNo)],
      code: 'Success',
      success: true,
    };
  } catch (e) {
    console.warn('[Fresh] 售后详情加载失败', e && e.message);
    return { data: [], success: false, msg: (e && e.message) || '加载失败' };
  }
}

export function cancelRights() {
  return Promise.resolve({
    data: {},
    code: 'Success',
    msg: null,
    requestId: mockReqId(),
    clientIp: mockIp(),
    rt: 79,
    success: true,
  });
}
