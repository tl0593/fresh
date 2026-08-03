import { config } from '../../config/index';
import request from '../../utils/request';
import { adaptGoodsDetail } from '../adapters/goods';

/** 获取商品详情 */
function mockFetchGood(ID = 0) {
  const { delay } = require('../_utils/delay');
  const { genGood } = require('../../model/good');
  return delay().then(() => genGood(ID));
}

/** 获取商品详情（图片原样返回，由页面 webp-image 组件桥接 http） */
export async function fetchGood(ID = 0) {
  if (config.useMock) {
    return mockFetchGood(ID);
  }
  const detail = await request.get(`/api/goods/goods/${ID}`);
  return adaptGoodsDetail(detail);
}
