import request, { getToken, setToken } from '../../utils/request';
import { config } from '../../config/index';

/**
 * 小程序登录：wx.login → Fresh /api/user/mini/login
 * mock 登录时 openid = wx_{code}
 */
export function miniLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: async (res) => {
        if (!res.code) {
          reject(new Error('wx.login 未返回 code'));
          return;
        }
        try {
          const data = await request.post(
            '/api/user/mini/login',
            { code: res.code },
            { auth: false },
          );
          setToken(data.token);
          if (data.userInfo) {
            wx.setStorageSync('userInfo', data.userInfo);
          }
          resolve(data);
        } catch (e) {
          reject(e);
        }
      },
      fail: (err) => reject(new Error(err.errMsg || 'wx.login 失败')),
    });
  });
}

/** 确保已登录；无 token 时自动登录 */
export async function ensureLogin() {
  if (!config.useFreshPay) {
    return null;
  }
  if (getToken()) {
    return { token: getToken() };
  }
  return miniLogin();
}

/**
 * 确保当前用户有收货地址；没有则创建默认自提地址（便于 mock 联调）
 */
export async function ensureDefaultAddress() {
  await ensureLogin();
  const list = await request.get('/api/user/address/list');
  if (list && list.length > 0) {
    return list.find((a) => a.isDefault === 1) || list[0];
  }
  await request.post('/api/user/address/save', {
    name: '自提用户',
    phone: '13800138000',
    community: '阳光社区自提点',
    detailAddr: '默认自提',
    isDefault: 1,
    delFlag: 0,
  });
  const again = await request.get('/api/user/address/list');
  return (again && again[0]) || null;
}
