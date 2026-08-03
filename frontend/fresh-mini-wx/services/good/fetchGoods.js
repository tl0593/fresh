import { config } from '../../config/index';
import request from '../../utils/request';
import { adaptGoodsCard, resolveGoodsImage } from '../adapters/goods';

/** 获取商品列表（首页） */
function mockFetchGoodsList(pageIndex = 1, pageSize = 20) {
  const { delay } = require('../_utils/delay');
  const { getGoodsList } = require('../../model/goods');
  return delay().then(() =>
    getGoodsList(pageIndex, pageSize).map((item) => ({
      spuId: item.spuId,
      thumb: item.primaryImage,
      title: item.title,
      price: item.minSalePrice,
      originPrice: item.maxLinePrice,
      tags: item.spuTagList.map((tag) => tag.title),
    })),
  );
}

/**
 * 首页商品列表
 * @param {number} pageIndex 页码或 tab 偏移
 * @param {number} pageSize 每页数量
 * @param {number} [tabKey] 0热销 1团购 2秒杀
 */
export async function fetchGoodsList(pageIndex = 1, pageSize = 20, tabKey = 0) {
  if (config.useMock) {
    return mockFetchGoodsList(pageIndex, pageSize);
  }

  if (tabKey === 1) {
    const list = (await request.get('/api/goods/group/list')) || [];
    const mapped = await Promise.all(
      list.slice(0, pageSize).map(async (g) => {
        let goodsName = g.goodsName;
        let goodsImg = g.goodsImg;
        let originPrice = g.originPrice;
        if ((!goodsName || !goodsImg) && g.goodsId) {
          try {
            const detail = await request.get(`/api/goods/goods/${g.goodsId}`);
            goodsName = goodsName || (detail && detail.goodsName);
            goodsImg = goodsImg || (detail && detail.goodsImg);
            originPrice = originPrice != null ? originPrice : detail && (detail.originPrice || detail.salePrice);
          } catch (e) {
            /* ignore */
          }
        }
        return {
          spuId: String(g.goodsId || g.id),
          thumb: resolveGoodsImage(goodsImg, g.goodsId || g.id),
          title: goodsName || g.groupDesc || g.activityName || '团购商品',
          price: Math.round(Number(g.groupPrice || g.salePrice || 0) * 100),
          originPrice: Math.round(Number(originPrice || g.salePrice || 0) * 100),
          tags: ['团购'],
          activityType: 2,
          activityId: g.id,
        };
      }),
    );
    return mapped;
  }
  if (tabKey === 2) {
    const list = (await request.get('/api/goods/seckill/list')) || [];
    const mapped = await Promise.all(
      list.slice(0, pageSize).map(async (g) => {
        let goodsName = g.goodsName;
        let goodsImg = g.goodsImg;
        let originPrice = g.originPrice;
        if ((!goodsName || !goodsImg) && g.goodsId) {
          try {
            const detail = await request.get(`/api/goods/goods/${g.goodsId}`);
            goodsName = goodsName || (detail && detail.goodsName);
            goodsImg = goodsImg || (detail && detail.goodsImg);
            originPrice = originPrice != null ? originPrice : detail && (detail.originPrice || detail.salePrice);
          } catch (e) {
            /* ignore */
          }
        }
        return {
          spuId: String(g.goodsId || g.id),
          thumb: resolveGoodsImage(goodsImg, g.goodsId || g.id),
          title: goodsName || g.activityName || '秒杀商品',
          price: Math.round(Number(g.seckillPrice || g.salePrice || 0) * 100),
          originPrice: Math.round(Number(originPrice || g.salePrice || 0) * 100),
          tags: ['秒杀'],
          activityType: 3,
          activityId: g.id,
        };
      }),
    );
    return mapped;
  }

  const hot = (await request.get('/api/goods/goods/hot')) || [];
  const start = Math.max(0, (Number(pageIndex) || 0) * pageSize);
  return hot.slice(start, start + pageSize).map(adaptGoodsCard).filter(Boolean);
}
