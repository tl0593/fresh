import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { fetchOrdersCount } from '../order/orderList';

function mockFetchUserCenter() {
  const { delay } = require('../_utils/delay');
  const { genUsercenter } = require('../../model/usercenter');
  return delay(200).then(() => genUsercenter());
}

export async function fetchUserCenter() {
  if (config.useMock) {
    return mockFetchUserCenter();
  }

  await ensureLogin();
  const cached = wx.getStorageSync('userInfo') || {};
  const orderCounts = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 };
  let couponCount = 0;
  let integral = cached.integral || 0;

  try {
    const me = await request.get('/api/user/integral/balance');
    if (me && me.integral != null) {
      integral = Number(me.integral) || 0;
      cached.integral = integral;
      wx.setStorageSync('userInfo', cached);
    }
  } catch (e) {
    /* ignore */
  }

  try {
    const countRes = await fetchOrdersCount();
    const countsArr = (countRes && countRes.data) || [];
    countsArr.forEach((c) => {
      orderCounts[c.tabType] = c.orderNum || 0;
    });
  } catch (e) {
    console.warn('[Fresh] 订单角标失败', e && e.message);
  }

  try {
    const mine = (await request.get('/api/goods/coupon/mine', { status: 0 })) || [];
    couponCount = mine.length;
  } catch (e) {
    /* ignore */
  }

  return {
    userInfo: {
      avatarUrl:
        cached.avatar ||
        cached.avatarUrl ||
        'https://we-retail-static-1300977798.cos.ap-guangzhou.myqcloud.com/retail-ui/components-exp/avatar/avatar-1.jpg',
      nickName: cached.nickName || cached.nickname || '生鲜用户',
      phoneNumber: cached.phone || '',
      gender: 0,
    },
    countsData: [
      { num: integral, name: '积分', type: 'point' },
      { num: couponCount, name: '优惠券', type: 'coupon' },
    ],
    orderTagInfos: [
      { orderNum: orderCounts[0] || 0, tabType: 0 },
      { orderNum: orderCounts[1] || 0, tabType: 1 },
      { orderNum: orderCounts[2] || 0, tabType: 2 },
      { orderNum: orderCounts[3] || 0, tabType: 3 },
      { orderNum: orderCounts[4] || 0, tabType: 4 },
    ],
    customerServiceInfo: {
      servicePhone: '4008002026',
      serviceTimeDuration: '每天 9:00-21:00',
    },
  };
}

export async function fetchIntegralLogs() {
  await ensureLogin();
  return (await request.get('/api/user/integral/log')) || [];
}
