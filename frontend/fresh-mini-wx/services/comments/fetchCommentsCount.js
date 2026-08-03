import { config } from '../../config/index';
import { getGoodsDetailsCommentsCount } from '../good/fetchGoodsDetailsComments';

/** 评价统计——兼容 { spuId } 或纯 id */
export async function fetchCommentsCount(spuIdOrParams = 0) {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getGoodsDetailsCommentsCount: mockCount } = require('../../model/detailsComments');
    const spuId =
      spuIdOrParams && typeof spuIdOrParams === 'object'
        ? spuIdOrParams.spuId
        : spuIdOrParams;
    return delay().then(() => mockCount(spuId));
  }
  return getGoodsDetailsCommentsCount(spuIdOrParams);
}
