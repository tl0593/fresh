import { config } from '../../../config/index';
import { getGoodsDetailsCommentList } from '../fetchGoodsDetailsComments';

export async function fetchCommentDetail(spuId = 0) {
  if (config.useMock) {
    const { delay } = require('../../_utils/delay');
    const { getGoodsDetailsComments } = require('../../../model/detailsComments');
    return delay().then(() => getGoodsDetailsComments(spuId));
  }
  return getGoodsDetailsCommentList(spuId);
}
