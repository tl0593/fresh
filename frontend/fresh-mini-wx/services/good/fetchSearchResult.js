/* eslint-disable no-param-reassign */
import { config } from '../../config/index';
import request from '../../utils/request';
import { adaptGoodsCard } from '../adapters/goods';

/** 搜索结果 */
function mockSearchResult(params) {
  const { delay } = require('../_utils/delay');
  const { getSearchResult } = require('../../model/search');

  const data = getSearchResult(params);

  if (data.spuList.length) {
    data.spuList.forEach((item) => {
      item.thumb = item.primaryImage;
      item.price = item.minSalePrice;
      item.originPrice = item.maxLinePrice;
      if (item.spuTagList) {
        item.tags = item.spuTagList.map((tag) => ({ title: tag.title }));
      } else {
        item.tags = [];
      }
    });
  }
  return delay().then(() => data);
}

/** 搜索（基于热销列表本地过滤） */
export async function getSearchResult(params = {}) {
  if (config.useMock) {
    return mockSearchResult(params);
  }

  const hot = (await request.get('/api/goods/goods/hot')) || [];
  let list = hot.map(adaptGoodsCard).filter(Boolean);
  const keyword = (params.keyword || params.words || '').trim();
  if (keyword) {
    list = list.filter((item) => (item.title || '').includes(keyword));
  }

  return {
    spuList: list,
    totalCount: list.length,
  };
}
