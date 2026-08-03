/* eslint-disable no-param-reassign */
import { config } from '../../config/index';
import request from '../../utils/request';
import { adaptGoodsCard } from '../adapters/goods';

/** 获取商品列表（分类/筛选） */
function mockFetchGoodsList(params) {
  const { delay } = require('../_utils/delay');
  const { getSearchResult } = require('../../model/search');

  const data = getSearchResult(params);

  if (data.spuList.length) {
    data.spuList.forEach((item) => {
      item.thumb = item.primaryImage;
      item.price = item.minSalePrice;
      item.originPrice = item.maxLinePrice;
      item.desc = '';
      if (item.spuTagList) {
        item.tags = item.spuTagList.map((tag) => tag.title);
      } else {
        item.tags = [];
      }
    });
  }
  return delay().then(() => data);
}

/**
 * 获取商品列表
 * 对接 GET /api/goods/goods/list
 */
export async function fetchGoodsList(params = {}) {
  if (config.useMock) {
    return mockFetchGoodsList(params);
  }

  const pageNum = Number(params.pageNum || params.pageIndex || 1);
  const pageSize = Number(params.pageSize || 20);
  const catId = params.categoryId || params.catId || params.groupId || '';
  const keyword = (params.keyword || params.words || '').trim();

  let sortType = params.sortType || 'sale';
  let sortOrder = params.sortOrder || 'desc';
  if (params.sort === 1 || params.sort === '1') {
    sortType = 'price';
    sortOrder = params.sortType === 0 || params.sortType === '0' ? 'asc' : 'desc';
  } else if (params.sort === 0 || params.sort === '0') {
    sortType = 'sale';
    sortOrder = 'desc';
  }

  const query = {
    pageNum,
    pageSize,
    sortType,
    sortOrder,
  };
  if (catId) query.catId = catId;
  if (keyword) query.keyword = keyword;

  const page = (await request.get('/api/goods/goods/list', query)) || {};
  const records = page.records || page.list || [];
  const spuList = records.map(adaptGoodsCard).filter(Boolean);

  return {
    spuList,
    totalCount: Number(page.total != null ? page.total : spuList.length),
    pageNum,
    pageSize,
  };
}
