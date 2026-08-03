import { config } from '../../config/index';

/** 搜索历史（本地存储） */
export function getSearchHistory() {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getSearchHistory } = require('../../model/search');
    return delay().then(() => getSearchHistory());
  }
  const history = wx.getStorageSync('fresh_search_history') || [];
  return Promise.resolve(history);
}

export function saveSearchHistory(keyword) {
  if (!keyword) return;
  const history = wx.getStorageSync('fresh_search_history') || [];
  const next = [keyword, ...history.filter((k) => k !== keyword)].slice(0, 10);
  wx.setStorageSync('fresh_search_history', next);
}

export function getSearchPopular() {
  if (config.useMock) {
    const { delay } = require('../_utils/delay');
    const { getSearchPopular } = require('../../model/search');
    return delay().then(() => getSearchPopular());
  }
  return Promise.resolve(['有机蔬菜', '新鲜水果', '今日团购', '限时秒杀']);
}
