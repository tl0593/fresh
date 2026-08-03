import { config } from '../../config/index';

/** 获取个人信息 */
function mockFetchPerson() {
  const { delay } = require('../_utils/delay');
  const { genSimpleUserInfo } = require('../../model/usercenter');
  return delay().then(() => ({
    ...genSimpleUserInfo(),
    address: {
      provinceName: '社区自提',
      provinceCode: '',
      cityName: '',
      cityCode: '',
    },
  }));
}

/** 获取个人信息 */
export function fetchPerson() {
  if (config.useMock) {
    return mockFetchPerson();
  }
  const cached = wx.getStorageSync('userInfo') || {};
  return Promise.resolve({
    avatarUrl:
      cached.avatar ||
      cached.avatarUrl ||
      'https://we-retail-static-1300977798.cos.ap-guangzhou.myqcloud.com/retail-ui/components-exp/avatar/avatar-1.jpg',
    nickName: cached.nickName || cached.nickname || '生鲜用户',
    phoneNumber: cached.phone || '',
    gender: 0,
    address: {
      provinceName: '社区自提',
      provinceCode: '',
      cityName: '',
      cityCode: '',
    },
  });
}
