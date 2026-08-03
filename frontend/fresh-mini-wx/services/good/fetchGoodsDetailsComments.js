import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';

function resolveSpuId(spuIdOrParams) {
  if (spuIdOrParams && typeof spuIdOrParams === 'object') {
    return spuIdOrParams.spuId || spuIdOrParams.goodsId || spuIdOrParams.queryParameter?.spuId || 0;
  }
  return spuIdOrParams || 0;
}

function mapCommentItem(item, spuId) {
  const images = (item.images || []).filter(Boolean);
  return {
    spuId: String(item.goodsId || spuId || ''),
    skuId: item.specId != null ? String(item.specId) : null,
    specInfo: null,
    commentContent: item.content || '',
    commentScore: item.score || 5,
    commentTime: item.createTime || '',
    uid: String(item.userId || ''),
    userName: `用户${item.userId || ''}`,
    userHeadUrl: '',
    isAnonymity: false,
    commentImageList: images,
    commentResources: images.map((src) => ({ type: 'image', src })),
    sellerReply: (item.reply && item.reply.replyContent) || '',
    goodsDetailInfo: item.goodsId ? `商品ID ${item.goodsId}` : '',
  };
}

function emptyStats() {
  return {
    commentCount: '0',
    badCount: '0',
    middleCount: '0',
    goodCount: '0',
    hasImageCount: '0',
    goodRate: 100,
    uidCount: '0',
  };
}

/** 获取商品详情页评论数 */
function mockFetchGoodDetailsCommentsCount(spuId = 0) {
  const { delay } = require('../_utils/delay');
  const { getGoodsDetailsCommentsCount } = require('../../model/detailsComments');
  return delay().then(() => getGoodsDetailsCommentsCount(spuId));
}

/** 获取商品详情页评论数 */
export async function getGoodsDetailsCommentsCount(spuIdOrParams = 0) {
  const spuId = resolveSpuId(spuIdOrParams);
  if (config.useMock) {
    return mockFetchGoodDetailsCommentsCount(spuId);
  }
  if (!spuId) {
    return emptyStats();
  }

  let total = 0;
  let goodRate = 100;
  let avgScore = 5;

  // 优先专用统计接口（权威总数）
  try {
    const rate = await request.get(`/api/goods/comment/stats/${spuId}`, {}, { auth: false });
    if (rate) {
      total = Number(rate.totalCount != null ? rate.totalCount : 0) || 0;
      if (rate.goodRate != null) goodRate = Number(rate.goodRate);
      if (rate.avgScore != null) avgScore = Number(rate.avgScore);
    }
  } catch (e) {
    console.warn('[Fresh] comment/stats 失败，回退 list', e && e.message);
  }

  // 回退：列表 total + 抽样细分
  let goodCount = 0;
  let middleCount = 0;
  let badCount = 0;
  let hasImageCount = 0;
  try {
    const data = await request.get(
      `/api/goods/comment/list/${spuId}`,
      { pageNum: 1, pageSize: 50 },
      { auth: false },
    );
    const records = (data && data.records) || [];
    const listTotal = Number(data && data.total != null ? data.total : records.length) || 0;
    if (listTotal > total) total = listTotal;
    if (data && data.goodRate != null) goodRate = Number(data.goodRate);
    if (data && data.avgScore != null) avgScore = Number(data.avgScore);

    records.forEach((item) => {
      const score = Number(item.score || 0);
      if (score >= 4) goodCount += 1;
      else if (score === 3) middleCount += 1;
      else badCount += 1;
      if ((item.images || []).length) hasImageCount += 1;
    });
    if (total > records.length && total > 0) {
      goodCount = Math.round((total * goodRate) / 100);
      badCount = Math.max(0, total - goodCount);
      middleCount = 0;
    } else if (total > 0 && goodCount + middleCount + badCount === 0) {
      goodCount = Math.round((total * goodRate) / 100);
      badCount = Math.max(0, total - goodCount);
    }
  } catch (e) {
    if (total > 0) {
      goodCount = Math.round((total * goodRate) / 100);
      badCount = Math.max(0, total - goodCount);
    } else {
      console.warn('[Fresh] 评价统计失败', spuId, e && e.message);
      return emptyStats();
    }
  }

  let uidCount = 0;
  try {
    await ensureLogin();
    const mine = (await request.get('/api/goods/comment/user/list')) || [];
    uidCount = mine.filter((c) => String(c.goodsId) === String(spuId)).length;
  } catch (e) {
    uidCount = 0;
  }

  return {
    commentCount: String(total),
    badCount: String(badCount),
    middleCount: String(middleCount),
    goodCount: String(goodCount),
    hasImageCount: String(hasImageCount),
    goodRate,
    avgScore,
    uidCount: String(uidCount),
  };
}

/** 获取商品详情页评论 */
function mockFetchGoodDetailsCommentList(spuId = 0) {
  const { delay } = require('../_utils/delay');
  const { getGoodsDetailsComments } = require('../../model/detailsComments');
  return delay().then(() => getGoodsDetailsComments(spuId));
}

/** 获取商品详情页评论 */
export async function getGoodsDetailsCommentList(spuIdOrParams = 0) {
  const spuId = resolveSpuId(spuIdOrParams);
  if (config.useMock) {
    return mockFetchGoodDetailsCommentList(spuId);
  }
  if (!spuId) {
    return { homePageComments: [], pageList: [], totalCount: 0 };
  }
  const pageNum =
    (spuIdOrParams && typeof spuIdOrParams === 'object' && spuIdOrParams.pageNum) || 1;
  const pageSize =
    (spuIdOrParams && typeof spuIdOrParams === 'object' && spuIdOrParams.pageSize) || 10;
  const data = await request.get(
    `/api/goods/comment/list/${spuId}`,
    { pageNum, pageSize },
    { auth: false },
  );
  const records = (data && data.records) || [];
  const mapped = records.map((item) => mapCommentItem(item, spuId));
  return {
    homePageComments: mapped,
    pageList: mapped,
    totalCount: Number(data && data.total != null ? data.total : mapped.length) || 0,
    goodRate: data && data.goodRate != null ? Number(data.goodRate) : undefined,
  };
}

/** 当前用户对某商品的评价（「自己」Tab） */
export async function getMyGoodsComments(spuId) {
  await ensureLogin();
  const mine = (await request.get('/api/goods/comment/user/list')) || [];
  const list = spuId
    ? mine.filter((c) => String(c.goodsId) === String(spuId))
    : mine;
  return list.map((item) => mapCommentItem(item, item.goodsId));
}

/** 当前用户全部评价 */
export async function getMyAllComments() {
  await ensureLogin();
  const mine = (await request.get('/api/goods/comment/user/list')) || [];
  return mine.map((item) => mapCommentItem(item, item.goodsId));
}
