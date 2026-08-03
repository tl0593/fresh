import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { adaptCouponTemplate, adaptUserCoupon, yuanToFen } from '../adapters/goods';

/** 获取优惠券列表 */
function mockFetchCoupon(status) {
  const { delay } = require('../_utils/delay');
  const { getCouponList } = require('../../model/coupon');
  return delay().then(() => getCouponList(status));
}

/**
 * 我的优惠券
 * status: default=未使用 / useless=已使用 / disabled=已过期
 */
export async function fetchCouponList(status = 'default') {
  if (config.useMock) {
    return mockFetchCoupon(status);
  }
  await ensureLogin();
  let useStatus = 0;
  if (status === 'useless') useStatus = 1;
  else if (status === 'disabled') useStatus = 2;
  const list = (await request.get('/api/goods/coupon/mine', { status: useStatus })) || [];
  return list.map(adaptUserCoupon).filter(Boolean);
}

/** 领券中心：可领模板列表 */
export async function fetchCouponCenterList() {
  if (config.useMock) {
    return mockFetchCoupon('default');
  }
  await ensureLogin();
  const list = (await request.get('/api/goods/coupon/template/list')) || [];
  return list
    .map((t) => {
      const item = adaptCouponTemplate(t);
      if (!item) return null;
      item.action = 'receive';
      item.remainCount = t.remainCount;
      return item;
    })
    .filter(Boolean);
}

/** 领取优惠券 */
export async function receiveCoupon(templateId) {
  await ensureLogin();
  return request.post('/api/goods/coupon/receive', { templateId: Number(templateId) });
}

/** 一键领取 */
export async function receiveCouponBatch() {
  await ensureLogin();
  return request.post('/api/goods/coupon/receive/batch', {});
}

function adaptSeckill(row) {
  if (!row) return null;
  const reduceFen = yuanToFen(row.reduceAmount);
  const fullFen = yuanToFen(row.fullAmount);
  return {
    id: row.id,
    templateId: row.templateId,
    title: row.couponName || '整点抢券',
    value: reduceFen,
    base: fullFen,
    startHour: row.startHour,
    remainStock: row.remainStock != null ? row.remainStock : Math.max(0, (row.totalStock || 0) - (row.usedNum || 0)),
    totalStock: row.totalStock || 0,
    grabStatus: row.grabStatus,
    grabStatusText: row.grabStatusText || '',
    canGrab: Number(row.grabStatus) === 1 && (row.remainStock == null || row.remainStock > 0),
  };
}

function adaptIntegralCoupon(row) {
  if (!row) return null;
  return {
    id: row.id,
    templateId: row.templateId,
    title: row.couponName || '积分兑换券',
    value: yuanToFen(row.reduceAmount),
    base: yuanToFen(row.fullAmount),
    costIntegral: row.costIntegral || 0,
    dailyLimit: row.dailyLimit || 0,
    remainStock: row.remainStock != null ? row.remainStock : Math.max(0, (row.totalStock || 0) - (row.usedNum || 0)),
    status: row.status,
  };
}

/** 整点抢券列表 */
export async function fetchSeckillCouponList() {
  await ensureLogin();
  const list = (await request.get('/api/goods/coupon/seckill/list')) || [];
  return list.map(adaptSeckill).filter(Boolean);
}

/** 整点抢券 */
export async function grabSeckillCoupon(actId) {
  await ensureLogin();
  return request.post('/api/goods/coupon/seckill/receive', { actId: Number(actId) });
}

/** 积分兑券列表 */
export async function fetchIntegralCouponList() {
  await ensureLogin();
  const list = (await request.get('/api/goods/integral/coupon/list')) || [];
  return list.map(adaptIntegralCoupon).filter(Boolean);
}

/** 积分兑换优惠券 */
export async function exchangeIntegralCoupon(integralCouponId) {
  await ensureLogin();
  return request.post('/api/goods/integral/coupon/exchange', {
    integralCouponId: Number(integralCouponId),
  });
}

/** 抽奖奖品池 */
export async function fetchLotteryPrizes() {
  await ensureLogin();
  return (await request.get('/api/goods/integral/lottery/prizes')) || [];
}

/** 积分抽奖 */
export async function drawLottery() {
  await ensureLogin();
  return request.post('/api/goods/integral/lottery/draw', {});
}

/** 获取优惠券详情 */
function mockFetchCouponDetail(id, status) {
  const { delay } = require('../_utils/delay');
  const { getCoupon } = require('../../model/coupon');
  const { genAddressList } = require('../../model/address');

  return delay().then(() => {
    const result = {
      detail: getCoupon(id, status),
      storeInfoList: genAddressList(),
    };
    result.detail.useNotes = `社区生鲜团购专用券，自提单可用`;
    result.detail.storeAdapt = `社区自提点通用`;
    if (result.detail.type === 'price') {
      result.detail.desc = `减免 ${result.detail.value / 100} 元`;
      if (result.detail.base) {
        result.detail.desc += `，满${result.detail.base / 100}元可用`;
      }
      result.detail.desc += '。';
    }
    return result;
  });
}

/** 获取优惠券详情 */
export async function fetchCouponDetail(id, status = 'default') {
  if (config.useMock) {
    return mockFetchCouponDetail(id, status);
  }
  await ensureLogin();
  try {
    const mine = (await request.get('/api/goods/coupon/mine')) || [];
    const foundMine = mine.find((c) => String(c.id) === String(id));
    if (foundMine) {
      const detail = adaptUserCoupon(foundMine);
      detail.useNotes = '社区生鲜团购专用券，自提单可用，不与其他活动冲突时以结算页为准';
      detail.storeAdapt = '社区自提点通用';
      detail.desc =
        detail.base > 0
          ? `减免 ${detail.value / 100} 元，满${detail.base / 100}元可用。`
          : `减免 ${detail.value / 100} 元，无门槛。`;
      return { detail, storeInfoList: [] };
    }
  } catch (e) {
    // ignore
  }
  const list = (await request.get('/api/goods/coupon/template/list')) || [];
  const found = list.find((t) => String(t.id) === String(id)) || list[0];
  const detail = adaptCouponTemplate(found);
  detail.useNotes = '社区生鲜团购专用券，自提单可用，不与其他活动冲突时以结算页为准';
  detail.storeAdapt = '社区自提点通用';
  detail.desc =
    detail.base > 0
      ? `减免 ${detail.value / 100} 元，满${detail.base / 100}元可用。`
      : `减免 ${detail.value / 100} 元，无门槛。`;
  return { detail, storeInfoList: [] };
}
