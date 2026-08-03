import request from '../../utils/request';
import { ensureLogin } from '../auth/login';

export async function fetchGroupList() {
  const list = (await request.get('/api/goods/group/list')) || [];
  return Promise.all(
    list.map(async (g) => {
      let title = g.goodsName || g.groupDesc;
      let thumb = g.goodsImg;
      if ((!title || !thumb) && g.goodsId) {
        try {
          const detail = await request.get(`/api/goods/goods/${g.goodsId}`);
          title = title || (detail && detail.goodsName);
          thumb = thumb || (detail && detail.goodsImg);
        } catch (e) {
          /* ignore */
        }
      }
      return {
        id: g.id,
        goodsId: g.goodsId,
        specId: g.specId,
        title: title || `团购活动 #${g.id}`,
        thumb,
        price: g.groupPrice,
        groupNum: g.groupNum,
        stock: g.stock,
        startTime: g.startTime,
        endTime: g.endTime,
        status: g.status,
        activityType: 2,
      };
    }),
  );
}

export async function fetchSeckillList() {
  const list = (await request.get('/api/goods/seckill/list')) || [];
  return Promise.all(
    list.map(async (g) => {
      let title = g.goodsName || g.activityName;
      let thumb = g.goodsImg;
      if ((!title || !thumb) && g.goodsId) {
        try {
          const detail = await request.get(`/api/goods/goods/${g.goodsId}`);
          title = title || (detail && detail.goodsName);
          thumb = thumb || (detail && detail.goodsImg);
        } catch (e) {
          /* ignore */
        }
      }
      return {
        id: g.id,
        goodsId: g.goodsId,
        specId: g.specId,
        title: title || `秒杀活动 #${g.id}`,
        thumb,
        price: g.seckillPrice,
        stock: g.stock,
        startTime: g.startTime,
        endTime: g.endTime,
        status: g.status,
        activityType: 3,
      };
    }),
  );
}

export async function sendAiChat({ sessionKey, userMsg, chatType = 1 }) {
  await ensureLogin();
  return request.post('/api/ai/ai/chat/send', {
    sessionKey: sessionKey || `s_${Date.now()}`,
    userMsg,
    chatType,
  });
}

export async function generateCook(preference) {
  await ensureLogin();
  return request.get('/api/ai/ai/cook/generate', { preference: preference || '' });
}

export async function fetchInnerMessages() {
  await ensureLogin();
  return (await request.get('/api/message/inner/list')) || [];
}

export async function markMessageRead(msgId) {
  await ensureLogin();
  return request.put(`/api/message/inner/read/${msgId}`);
}
