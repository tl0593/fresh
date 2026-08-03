import { config } from '../../config/index';
import request from '../../utils/request';
import { adaptCategoryTree } from '../adapters/goods';

/** 获取商品分类 */
function mockFetchGoodCategory() {
  const { delay } = require('../_utils/delay');
  const { getCategoryList } = require('../../model/category');
  return delay().then((list) => ({ list: getCategoryList(), level: 3 }));
}

/** 获取商品分类树，返回 { list, level } */
export async function getCategoryList() {
  if (config.useMock) {
    return mockFetchGoodCategory();
  }
  const tree = await request.get('/api/goods/category/tree');
  return adaptCategoryTree(tree || []);
}
