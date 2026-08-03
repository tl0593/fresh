import { config } from '../../config/index';
import request from '../../utils/request';

export async function fetchActivity(id) {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getActivity } = require('../../model/activities');
    return delay().then(() => getActivity(id));
  }
  try {
    const list = (await request.get('/api/goods/group/list')) || [];
    return list.find((g) => String(g.id) === String(id)) || list[0] || {};
  } catch (e) {
    return {};
  }
}
