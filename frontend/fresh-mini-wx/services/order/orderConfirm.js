import { config } from '../../config/index';
import { mockIp, mockReqId } from '../../utils/mock';
import request from '../../utils/request';
import { ensureLogin, ensureDefaultAddress } from '../auth/login';
import { yuanToFen, adaptAddress } from '../adapters/goods';
import { removeCartItems } from '../cart/cart';

/** 获取结算mock数据 */
function mockFetchSettleDetail(params) {
  const { delay } = require('../_utils/delay');
  const { genSettleDetail } = require('../../model/order/orderConfirm');

  return delay().then(() => genSettleDetail(params));
}

/** 提交mock订单 */
function mockDispatchCommitPay() {
  const { delay } = require('../_utils/delay');

  return delay().then(() => ({
    data: {
      isSuccess: true,
      tradeNo: '350930961469409099',
      payInfo: '{}',
      code: null,
      transactionId: 'E-200915180100299000',
      msg: null,
      interactId: '15145',
      channel: 'wechat',
      limitGoodsList: null,
    },
    code: 'Success',
    msg: null,
    requestId: mockReqId(),
    clientIp: mockIp(),
    rt: 891,
    success: true,
  }));
}

/** 组装下单/计价请求行：只传 id/数量/活动，价格由服务端计算 */
function mapGoodsToOrderItems(goodsRequestList) {
  return (goodsRequestList || []).map((g) => ({
    goodsId: Number(g.goodsId || g.spuId),
    specId: Number(g.specId || g.skuId),
    goodsName: g.goodsName || g.title || '商品',
    goodsImg: g.goodsImg || g.image || g.primaryImage || '',
    price: 0,
    num: Number(g.quantity || g.num || 1),
    activityType: g.activityType || 1,
    activityId: g.activityId || null,
  }));
}

function resolveSelectedUserCouponId(couponList) {
  if (!couponList || !couponList.length) return null;
  const hit = couponList.find((c) => c && (c.couponId || c.id));
  if (!hit) return null;
  const id = Number(hit.couponId || hit.id);
  return Number.isNaN(id) ? null : id;
}

function adaptAvailableCoupons(list, selectedId) {
  return (list || []).map((c) => {
    const reduceFen = yuanToFen(c.reduceAmount);
    const fullYuan = Number(c.fullAmount || 0);
    return {
      key: String(c.id),
      couponId: c.id,
      title: c.couponName || '优惠券',
      type: 1,
      value: reduceFen,
      desc: fullYuan > 0 ? `满${fullYuan}元可用` : '无门槛使用',
      status: 'default',
      timeLimit: c.validEnd ? String(c.validEnd).replace('T', ' ').slice(0, 16) : '长期有效',
      isSelected: selectedId != null && Number(selectedId) === Number(c.id),
      storeId: '1',
    };
  });
}

function buildSettleFromServer(settle, address, goodsRequestList, selectedUserCouponId) {
  const items = settle.items || [];
  // 后端金额单位：元；结算页 <price> 默认按「分」展示
  const totalSaleFen = yuanToFen(settle.totalAmount || 0);
  const totalPayFen = yuanToFen(settle.payAmount || settle.totalAmount || 0);
  const couponDeductFen = yuanToFen(settle.couponDeduct || 0);
  const promotionDeductFen = yuanToFen(settle.fullreduceDeduct || 0);
  const totalGoodsCount = Number(settle.totalGoodsCount || items.reduce((s, it) => s + (it.num || 0), 0));
  const availableCoupons = adaptAvailableCoupons(
    settle.availableCoupons,
    settle.selectedUserCouponId || selectedUserCouponId,
  );
  const selectedCoupons = availableCoupons.filter((c) => c.isSelected);

  const skuDetailVos = items.map((it, idx) => {
    const g = goodsRequestList[idx] || {};
    const priceFen = yuanToFen(it.price);
    return {
      ...g,
      goodsId: it.goodsId,
      spuId: it.goodsId,
      specId: it.specId,
      skuId: it.specId,
      storeId: g.storeId || '1',
      image: it.goodsImg || g.image || g.primaryImage || '',
      primaryImage: it.goodsImg || g.primaryImage || g.image || '',
      goodsName: it.goodsName || g.goodsName || g.title,
      title: it.goodsName || g.title || g.goodsName,
      settlePrice: priceFen,
      price: priceFen,
      quantity: it.num,
      num: it.num,
      activityType: it.activityType,
      activityId: it.activityId,
      skuSpecLst: g.skuSpecLst || g.specInfo || [],
    };
  });

  const adaptedAddress = address ? adaptAddress(address) : null;

  return {
    data: {
      settleType: adaptedAddress ? 1 : 0,
      userAddress: adaptedAddress,
      totalGoodsCount,
      totalAmount: String(totalSaleFen),
      totalSalePrice: String(totalSaleFen),
      totalPayAmount: String(totalPayFen),
      totalDiscountAmount: String(couponDeductFen + promotionDeductFen),
      totalPromotionAmount: String(promotionDeductFen),
      totalCouponAmount: String(couponDeductFen),
      selectedUserCouponId: settle.selectedUserCouponId || selectedUserCouponId || null,
      availableCoupons,
      storeGoodsList: [
        {
          storeId: '1',
          storeName: '社区自提点',
          storeTotalPayAmount: String(totalPayFen),
          skuDetailVos,
          couponList: selectedCoupons,
        },
      ],
      inValidGoodsList: null,
      outOfStockGoodsList: null,
      limitGoodsList: null,
      abnormalDeliveryGoodsList: null,
    },
    code: 'Success',
    success: true,
  };
}

/** 获取结算数据（服务端计价，含优惠券） */
export async function fetchSettleDetail(params) {
  if (config.useMock) {
    const res = await mockFetchSettleDetail(params);
    if (config.useFreshPay) {
      try {
        await ensureLogin();
        const address = params.userAddressReq || (await ensureDefaultAddress());
        if (address) {
          res.data.settleType = 1;
          res.data.userAddress = {
            ...address,
            id: address.id,
            name: address.name,
            phone: address.phone,
            address: `${address.community || ''}${address.detailAddr || ''}`,
            community: address.community,
            detailAddr: address.detailAddr,
          };
        }
      } catch (e) {
        console.warn('[Fresh] 注入真实地址失败', e && e.message);
      }
    }
    return res;
  }

  await ensureLogin();
  const address = params.userAddressReq || (await ensureDefaultAddress());
  const goodsRequestList = params.goodsRequestList || [];
  const items = mapGoodsToOrderItems(goodsRequestList);
  const userCouponId = resolveSelectedUserCouponId(params.couponList);
  const settle = await request.post('/api/order/order/settle', {
    items,
    userCouponId,
  });
  return buildSettleFromServer(settle, address, goodsRequestList, userCouponId);
}

/* 提交订单：创建 Fresh 待支付订单（金额由服务端重算） */
export async function dispatchCommitPay(params) {
  if (!config.useFreshPay && config.useMock) {
    return mockDispatchCommitPay(params);
  }

  await ensureLogin();
  let address = params.userAddressReq;
  if (!address || !address.id) {
    address = await ensureDefaultAddress();
  }
  if (!address || !address.id) {
    const err = new Error('请先添加收货地址');
    err.code = 'NO_ADDRESS';
    err.msg = err.message;
    throw err;
  }

  const items = mapGoodsToOrderItems(params.goodsRequestList);
  if (!items.length) {
    const err = new Error('结算商品为空');
    err.code = 'EMPTY_GOODS';
    err.msg = err.message;
    throw err;
  }

  try {
    const data = await request.post('/api/order/order/create', {
      addressId: Number(address.id),
      integralUsed: 0,
      userCouponId: resolveSelectedUserCouponId(params.couponList),
      items,
    });
    // 下单成功后清除购物车对应商品（来自购物车结算时）
    try {
      await removeCartItems(params.goodsRequestList);
      wx.removeStorageSync('order.goodsRequestList');
    } catch (e) {
      console.warn('[Fresh] 清理购物车失败', e && e.message);
    }
    return {
      data: {
        isSuccess: true,
        tradeNo: data.orderNo,
        payInfo: '{}',
        transactionId: null,
        interactId: null,
        channel: 'wechat',
        limitGoodsList: null,
        settleType: 1,
      },
      code: 'Success',
      msg: null,
      success: true,
    };
  } catch (e) {
    const err = new Error(e.message || '下单失败');
    err.code = 'ORDER_PAY_FAIL';
    err.msg = err.message;
    throw err;
  }
}

/** 开发票 */
export function dispatchSupplementInvoice() {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    return delay();
  }

  return Promise.resolve();
}
