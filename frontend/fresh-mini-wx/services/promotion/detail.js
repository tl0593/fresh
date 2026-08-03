import { config } from '../../config/index';
import { fetchGroupList } from '../fresh/index';

export async function fetchPromotion(id) {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getPromotion } = require('../../model/promotion');
    return delay().then(() => getPromotion(id));
  }
  const list = await fetchGroupList();
  const found = list.find((g) => String(g.id) === String(id)) || list[0];
  return {
    promotionId: found ? found.id : id,
    title: found ? found.title : '生鲜活动',
    goodsList: found
      ? [{ spuId: found.goodsId, title: found.title, price: Math.round(Number(found.price || 0) * 100) }]
      : [],
  };
}
