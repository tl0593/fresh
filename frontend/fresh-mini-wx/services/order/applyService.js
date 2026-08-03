import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { yuanToFen } from '../adapters/goods';
import { uploadImage } from './orderSubmitComment';

function mockFetchRightsPreview(params) {
  const { delay } = require('../_utils/delay');
  const { genRightsPreview } = require('../../model/order/applyService');
  return delay().then(() => genRightsPreview(params));
}

/**
 * 解析最大可退金额（分）
 * 优先订单实付 payAmount；兼容入口传入的 payAmt（已是分）
 */
function resolveRefundableFen(order, params) {
  const payYuan = order && order.payAmount;
  if (payYuan != null && payYuan !== '') {
    const fen = yuanToFen(payYuan);
    if (fen > 0) return fen;
  }
  const payAmtFen = Number(params && params.payAmt);
  if (!Number.isNaN(payAmtFen) && payAmtFen > 0) {
    return Math.round(payAmtFen);
  }
  return 0;
}

function pickOrderItem(items, params) {
  const list = items || [];
  const orderItemId = params && params.orderItemId;
  const skuId = params && params.skuId;
  const spuId = params && params.spuId;
  if (orderItemId) {
    const byId = list.find((it) => String(it.id) === String(orderItemId));
    if (byId) return byId;
  }
  if (skuId != null && skuId !== '') {
    const bySku = list.find((it) => String(it.specId) === String(skuId));
    if (bySku) return bySku;
  }
  if (spuId != null && spuId !== '') {
    const bySpu = list.find((it) => String(it.goodsId) === String(spuId));
    if (bySpu) return bySpu;
  }
  return list[0] || {};
}

/** 微信本地临时文件也会以 http://tmp/ 开头，不能当远程 URL */
export function isRemoteUploadedUrl(path) {
  const s = String(path || '').trim();
  if (!s) return false;
  if (
    s.indexOf('http://tmp') === 0 ||
    s.indexOf('https://tmp') === 0 ||
    s.indexOf('wxfile://') === 0 ||
    s.indexOf('file://') === 0
  ) {
    return false;
  }
  return s.indexOf('http://') === 0 || s.indexOf('https://') === 0;
}

/** 从上传组件 file 对象取出可用于 wx.uploadFile 的本地路径 */
export function resolveUploadLocalPath(file) {
  if (!file) return '';
  if (typeof file === 'string') return file;
  // tempFilePath / path 优先；url 可能是 http://tmp/xxx
  return file.tempFilePath || file.path || file.url || '';
}

/** 售后预览：无专用接口时用订单详情兜底，结构对齐申请页 */
export async function fetchRightsPreview(params) {
  if (config.useMock) {
    return mockFetchRightsPreview(params);
  }
  await ensureLogin();
  const orderNo = params && (params.orderNo || params.tradeNo);
  const skuId = params && params.skuId;
  let refundableAmount = resolveRefundableFen(null, params);
  let paidAmountEach = 0;
  let goodsInfo = {
    skuImage: '',
    goodsName: '生鲜商品',
    specInfo: [],
  };
  let goodsList = [];
  let orderItemId = (params && params.orderItemId) || null;
  let spuId = params && params.spuId;
  let boughtQuantity = Number((params && params.num) || 1) || 1;

  try {
    if (orderNo) {
      const detail = await request.get(`/api/order/order/${orderNo}`);
      const order = (detail && detail.order) || detail || {};
      const items = (detail && detail.items) || [];
      const item = pickOrderItem(items, params);

      refundableAmount = resolveRefundableFen(order, params);
      paidAmountEach = yuanToFen(item.price);
      boughtQuantity = Number(item.num || boughtQuantity || 1);
      orderItemId = item.id || orderItemId;
      spuId = item.goodsId || spuId;
      goodsInfo = {
        skuImage: item.goodsImg || '',
        goodsName: item.goodsName || '生鲜商品',
        specInfo: [{ specValue: '默认规格' }],
      };
      goodsList = items.map((it) => ({
        orderItemId: it.id,
        goodsId: it.goodsId,
        goodsName: it.goodsName || '生鲜商品',
        num: Number(it.num || 1),
      }));
      if (!goodsList.length && goodsInfo.goodsName) {
        goodsList = [{ goodsName: goodsInfo.goodsName, num: boughtQuantity, orderItemId }];
      }

      if (!refundableAmount) {
        const lineYuan =
          item.subTotal != null
            ? item.subTotal
            : Number(item.price || 0) * Number(item.num || 1);
        refundableAmount = yuanToFen(lineYuan);
      }
    }
  } catch (e) {
    console.warn('[Fresh] 售后预览失败', e && e.message);
  }

  return {
    data: {
      skuId: skuId || '',
      spuId: spuId || '',
      orderItemId,
      goodsInfo,
      goodsList,
      paidAmountEach,
      boughtQuantity,
      refundableAmount,
      numOfSku: boughtQuantity,
      numOfSkuAvailable: boughtQuantity,
      shippingFeeIncluded: 0,
    },
    success: true,
  };
}

export function dispatchConfirmReceived() {
  return Promise.resolve({ success: true, mocked: true });
}

export function fetchApplyReasonList() {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { genApplyReasonList } = require('../../model/order/applyService');
    return delay().then(() => genApplyReasonList());
  }
  return Promise.resolve({
    data: {
      rightsReasonList: [
        { id: 1, desc: '商品破损/坏果' },
        { id: 2, desc: '少发/漏发' },
        { id: 3, desc: '与描述不符' },
        { id: 4, desc: '其他' },
      ],
    },
    success: true,
  });
}

export async function dispatchApplyService(params) {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { applyService } = require('../../model/order/applyService');
    return delay().then(() => applyService(params));
  }

  await ensureLogin();
  const rights = (params && params.rights) || {};
  const item = ((params && params.rightsItem) || [])[0] || {};

  // 凭证图：本地临时路径必须上传；多图用 | 分隔（避免 URL 内逗号被拆开）
  const damageUrls = [];
  const seen = new Set();
  for (const f of rights.rightsImageUrls || []) {
    const localPath = resolveUploadLocalPath(f);
    if (!localPath || seen.has(localPath)) continue;
    seen.add(localPath);

    if (isRemoteUploadedUrl(localPath) && /\.(png|jpe?g|webp|gif)(\?|$)/i.test(localPath)) {
      damageUrls.push(localPath);
      continue;
    }
    try {
      const uploaded = await uploadImage(localPath, 'afterSale');
      const url = typeof uploaded === 'string' ? uploaded : (uploaded && uploaded.url) || '';
      if (!url || !isRemoteUploadedUrl(url) || !/\.(png|jpe?g|webp|gif)(\?|$)/i.test(url)) {
        throw new Error('上传成功但未返回可访问地址');
      }
      // 防御：历史上 dir 重复导致 afterSale,afterSale
      damageUrls.push(url.replace(/\/(afterSale|comment|goods),\1\//g, '/$1/'));
    } catch (e) {
      console.warn('[Fresh] 售后图上传失败', e && e.message);
      throw new Error((e && e.message) || '凭证图片上传失败，请重试');
    }
  }
  const damageImg = damageUrls.join('|');

  let orderItemId = item.orderItemId || rights.orderItemId || null;
  if (!orderItemId && rights.orderNo) {
    try {
      const preview = await fetchRightsPreview({
        orderNo: rights.orderNo,
        orderItemId: item.orderItemId,
        skuId: item.skuId,
        spuId: item.spuId,
        payAmt: rights.payAmt,
      });
      orderItemId = preview.data && preview.data.orderItemId;
    } catch (e) {
      /* ignore */
    }
  }

  const refundFen = Number(rights.refundRequestAmount || 0);
  const refundYuan = refundFen > 0 ? (refundFen / 100).toFixed(2) : '';
  const userRemark = rights.rightsReasonDesc || (params && params.refundMemo) || '';
  const remark = refundYuan
    ? `申请退款¥${refundYuan}${userRemark ? `；${userRemark}` : ''}`
    : userRemark;

  await request.post('/api/order/afterSale/apply', {
    orderItemId,
    goodsId: Number(item.spuId || item.goodsId) || null,
    damageImg,
    remark,
    delFlag: 0,
  });

  // 详情页优先用售后工单 id；此处先返回订单号，列表进入会用 mine 接口匹配
  return {
    data: { rightsNo: rights.orderNo || `AS_${Date.now()}` },
    success: true,
  };
}
