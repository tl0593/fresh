import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { adaptAddress } from '../adapters/goods';

/** 获取收货地址 */
function mockFetchDeliveryAddress(id) {
  const { delay } = require('../_utils/delay');
  const { genAddress } = require('../../model/address');
  return delay().then(() => genAddress(id));
}

/** 获取自提地址详情 */
export async function fetchDeliveryAddress(id = 0) {
  if (config.useMock) {
    return mockFetchDeliveryAddress(id);
  }
  await ensureLogin();
  const list = (await request.get('/api/user/address/list')) || [];
  const found = list.find((a) => String(a.id) === String(id));
  return adaptAddress(found || list[0]);
}

/** 获取收货地址列表 */
function mockFetchDeliveryAddressList(len = 0) {
  const { delay } = require('../_utils/delay');
  const { genAddressList } = require('../../model/address');

  return delay().then(() =>
    genAddressList(len).map((address) => ({
      ...address,
      phoneNumber: address.phone,
      address: `${address.provinceName}${address.cityName}${address.districtName}${address.detailAddress}`,
      tag: address.addressTag,
    })),
  );
}

/** 获取自提地址列表 */
export async function fetchDeliveryAddressList() {
  if (config.useMock) {
    return mockFetchDeliveryAddressList(10);
  }
  await ensureLogin();
  const list = (await request.get('/api/user/address/list')) || [];
  return list.map(adaptAddress).filter(Boolean);
}

/** 保存自提地址 */
export async function saveDeliveryAddress(address) {
  await ensureLogin();
  const payload = {
    id: address.id || address.addressId || undefined,
    name: address.name,
    phone: address.phone || address.phoneNumber,
    community: address.community || address.districtName || address.provinceName || '',
    detailAddr: address.detailAddr || address.detailAddress || '',
    isDefault: address.isDefault === 1 || address.isDefault === true ? 1 : 0,
    delFlag: 0,
  };
  await request.post('/api/user/address/save', payload);
  return payload;
}
