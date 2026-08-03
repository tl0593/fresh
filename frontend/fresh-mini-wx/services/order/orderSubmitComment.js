import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { getToken } from '../../utils/request';
import { resolveGoodsImage } from '../adapters/goods';

/** 获取评价商品 */
function mockGetGoods(parameter) {
  const { delay } = require('../_utils/delay');
  const { getGoods } = require('../../model/submitComment');
  return delay().then(() => getGoods(parameter));
}

function isItemCommented(it) {
  const v = it && it.isCommented;
  return v === 1 || v === true || v === '1';
}

/** 获取订单下商品（支持只返回待评价） */
export async function getGoods(parameter) {
  if (config.useMock) {
    return mockGetGoods(parameter);
  }
  const orderNo = parameter && (parameter.orderNo || parameter.tradeNo);
  if (!orderNo) {
    return { goodsList: [], pendingList: [] };
  }
  await ensureLogin();
  const detail = await request.get(`/api/order/order/${orderNo}`);
  const items = (detail && detail.items) || [];
  let doneSet = new Set();
  try {
    const done = (await request.get(`/api/goods/comment/order/${encodeURIComponent(orderNo)}/done`)) || [];
    doneSet = new Set((done || []).map((id) => String(id)));
  } catch (e) {
    /* 旧后端无此接口时忽略 */
  }
  const goodsList = items.map((it) => ({
    orderItemId: it.id,
    goodsId: it.goodsId,
    spuId: it.goodsId,
    title: it.goodsName || '商品',
    thumb: resolveGoodsImage(it.goodsImg, it.goodsId),
    skuId: it.specId,
    specText: it.specName || it.specValue || '',
    isCommented: isItemCommented(it) || doneSet.has(String(it.id)) ? 1 : 0,
  }));
  const pendingList = goodsList.filter((g) => !g.isCommented);
  return { goodsList, pendingList };
}

/** 提交评价（按订单项/商品） */
export async function submitComment({ orderItemId, score, content, images }) {
  await ensureLogin();
  return request.post('/api/goods/comment/submit', {
    orderItemId: Number(orderItemId),
    score: Number(score || 5),
    content: content || '',
    images: images || [],
  });
}

/** 上传图片（multipart）；dir 只放 formData */
export function uploadImage(filePath, dir = 'comment') {
  const { config: cfg } = require('../../config/index');
  const safeDir = String(dir || 'comment').replace(/[^a-zA-Z0-9_-]/g, '') || 'comment';
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${cfg.baseURL}/api/goods/upload/image`,
      filePath,
      name: 'file',
      formData: {
        dir: safeDir,
      },
      header: {
        Authorization: getToken() || '',
      },
      success(res) {
        try {
          const body = JSON.parse(res.data || '{}');
          if (body.code === 200) {
            const data = body.data;
            const url = typeof data === 'string' ? data : data && data.url;
            if (!url) {
              reject(new Error('上传成功但未返回图片地址'));
              return;
            }
            resolve({ url: String(url) });
            return;
          }
          reject(new Error(body.msg || '上传失败'));
        } catch (e) {
          reject(new Error('上传响应解析失败'));
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '上传失败'));
      },
    });
  });
}

export { isItemCommented };
