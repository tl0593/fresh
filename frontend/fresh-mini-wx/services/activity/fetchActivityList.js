import { config } from '../../config/index';
import request from '../../utils/request';

/** 活动列表：对接团购/秒杀简要信息供详情页展示 */
export async function fetchActivityList() {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getActivityList } = require('../../model/activities');
    return delay().then(() => getActivityList(1, 20));
  }
  try {
    const [group, seckill] = await Promise.all([
      request.get('/api/goods/group/list').catch(() => []),
      request.get('/api/goods/seckill/list').catch(() => []),
    ]);
    const list = [];
    (group || []).slice(0, 3).forEach((g) => {
      list.push({
        promotionSubCode: 'GROUP',
        promotionId: String(g.id),
        tag: '团购',
        label: g.groupDesc || `满${g.groupNum || 2}人成团`,
      });
    });
    (seckill || []).slice(0, 3).forEach((g) => {
      list.push({
        promotionSubCode: 'SECKILL',
        promotionId: String(g.id),
        tag: '秒杀',
        label: '限时秒杀进行中',
      });
    });
    return list;
  } catch (e) {
    return [];
  }
}
