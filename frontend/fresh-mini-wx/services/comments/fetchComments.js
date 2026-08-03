import { config } from '../../config/index';
import {
  getGoodsDetailsCommentList,
  getMyGoodsComments,
} from '../good/fetchGoodsDetailsComments';

/** 评价列表（商品页）——兼容 page 传入 params 对象或 spuId */
export async function fetchComments(spuIdOrParams = 0) {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getGoodsDetailsComments } = require('../../model/detailsComments');
    const spuId =
      spuIdOrParams && typeof spuIdOrParams === 'object'
        ? spuIdOrParams.queryParameter?.spuId || spuIdOrParams.spuId
        : spuIdOrParams;
    return delay().then(() => getGoodsDetailsComments(spuId));
  }
  return getGoodsDetailsCommentList(spuIdOrParams);
}

/** 当前用户在该商品下的评价 */
export async function fetchMyComments(spuId) {
  const list = await getMyGoodsComments(spuId);
  return { pageList: list, totalCount: list.length };
}
