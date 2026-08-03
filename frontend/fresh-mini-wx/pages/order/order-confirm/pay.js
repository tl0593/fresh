import Dialog from 'tdesign-miniprogram/dialog/index';
import Toast from 'tdesign-miniprogram/toast/index';

import { dispatchCommitPay } from '../../../services/order/orderConfirm';
import { yuanToFen } from '../../../services/adapters/goods';
import request from '../../../utils/request';
import { config } from '../../../config/index';

function resolvePayAmtFen(prepayAmount, fallbackFen) {
  if (prepayAmount === undefined || prepayAmount === null || prepayAmount === '') {
    return fallbackFen;
  }
  // 预支付接口返回元；结算页传入的 fallback 已是分
  return yuanToFen(prepayAmount);
}

// 真实的提交支付（创建订单）
export const commitPay = (params) => {
  return dispatchCommitPay({
    goodsRequestList: params.goodsRequestList,
    invoiceRequest: params.invoiceRequest,
    userAddressReq: params.userAddressReq,
    currency: params.currency || 'CNY',
    logisticsType: params.logisticsType || 1,
    orderType: params.orderType || 0,
    payType: params.payType || 1,
    totalAmount: params.totalAmount,
    userName: params.userName,
    payWay: 1,
    authorizationCode: '',
    storeInfoList: params.storeInfoList,
    couponList: params.couponList,
    groupInfo: params.groupInfo,
  });
};

export const paySuccess = (payOrderInfo) => {
  const { payAmt, tradeNo, groupId, promotionId } = payOrderInfo;
  Toast({
    context: this,
    selector: '#t-toast',
    message: '支付成功',
    duration: 2000,
    icon: 'check-circle',
  });

  const params = {
    totalPaid: payAmt,
    orderNo: tradeNo,
  };
  if (groupId) {
    params.groupId = groupId;
  }
  if (promotionId) {
    params.promotionId = promotionId;
  }
  const paramsStr = Object.keys(params)
    .map((k) => `${k}=${params[k]}`)
    .join('&');
  wx.redirectTo({ url: `/pages/order/pay-result/index?${paramsStr}` });
};

export const payFail = (payOrderInfo, resultMsg) => {
  if (resultMsg === 'requestPayment:fail cancel') {
    if (payOrderInfo.dialogOnCancel) {
      Dialog.confirm({
        title: '是否放弃付款',
        content: '商品可能很快就会被抢空哦，是否放弃付款？',
        confirmBtn: '放弃',
        cancelBtn: '继续付款',
      }).then(() => {
        wx.redirectTo({ url: '/pages/order/order-list/index' });
      });
    } else {
      Toast({
        context: this,
        selector: '#t-toast',
        message: '支付取消',
        duration: 2000,
        icon: 'close-circle',
      });
    }
  } else {
    Toast({
      context: this,
      selector: '#t-toast',
      message: `支付失败：${resultMsg}`,
      duration: 2000,
      icon: 'close-circle',
    });
    setTimeout(() => {
      wx.redirectTo({ url: '/pages/order/order-list/index' });
    }, 2000);
  }
};

/**
 * 微信支付：
 * - useFreshPay + 预支付 mock=true → 调 /pay/callback 模拟成功
 * - mock=false → wx.requestPayment（需商户号 / P3）
 * - 未开启 useFreshPay 时仍走模板直接成功（仅本地 demo）
 */
export const wechatPayOrder = async (payOrderInfo) => {
  const orderNo = payOrderInfo.tradeNo || payOrderInfo.orderId;

  if (!config.useFreshPay) {
    paySuccess(payOrderInfo);
    return;
  }

  try {
    const prepay = await request.post('/api/order/order/pay/prepay', { orderNo });

    if (prepay.mock === 'true' || prepay.mock === true) {
      await request.post('/api/order/order/pay/callback', {
        orderNo,
        transactionId: `mock_tx_${Date.now()}`,
      });
      paySuccess({
        ...payOrderInfo,
        payAmt: resolvePayAmtFen(prepay.payAmount, payOrderInfo.payAmt),
        tradeNo: orderNo,
      });
      return;
    }

    await new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: prepay.timeStamp,
        nonceStr: prepay.nonceStr,
        package: prepay.package,
        signType: prepay.signType || 'RSA',
        paySign: prepay.paySign,
        success() {
          // 入账以微信异步通知为准；此处仅跳转结果页
          paySuccess({
            ...payOrderInfo,
            payAmt: resolvePayAmtFen(prepay.payAmount, payOrderInfo.payAmt),
            tradeNo: orderNo,
          });
          resolve();
        },
        fail(err) {
          payFail(payOrderInfo, err.errMsg);
          reject(err);
        },
      });
    });
  } catch (e) {
    payFail(payOrderInfo, e.message || '预支付失败');
  }
};
