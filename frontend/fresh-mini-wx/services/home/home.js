import { config, cdnBase } from '../../config/index';
import request from '../../utils/request';
import { resolveGoodsImage, yuanToFen, adaptGoodsCard } from '../adapters/goods';

const DEFAULT_CAT_ICON =
  'https://tdesign.gtimg.com/miniprogram/template/retail/category/category-default.png';

const DISCOUNT_LIMIT = 10;

/** 随机打乱后取前 n 条 */
function shuffleTake(list, n) {
  const arr = (list || []).slice();
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    const t = arr[i];
    arr[i] = arr[j];
    arr[j] = t;
  }
  return arr.slice(0, n);
}

function formatPriceLabel(yuan) {
  return Number(yuan || 0)
    .toFixed(2)
    .replace(/\.00$/, '')
    .replace(/(\.\d)0$/, '$1');
}

function mockFetchHome() {
  const { delay } = require('../_utils/delay');
  const { genSwiperImageList } = require('../../model/swiper');
  const imgs = genSwiperImageList();
  return delay().then(() => ({
    discountList: Array.from({ length: 10 }).map((_, i) => ({
      id: String(i + 1),
      spuId: String(i + 1),
      title: `秒杀商品${i + 1}`,
      thumb: imgs[i % imgs.length],
      price: 590 + i * 100,
      priceLabel: ((590 + i * 100) / 100).toFixed(1),
      originPrice: 1990,
      activityId: i + 1,
    })),
    categories: [
      { id: '1', name: '新鲜蔬菜', icon: DEFAULT_CAT_ICON },
      { id: '4', name: '新鲜水果', icon: DEFAULT_CAT_ICON },
    ],
    tabList: [
      { text: '猜你喜欢', key: 0 },
      { text: '今日团购', key: 1 },
      { text: '限时秒杀', key: 2 },
    ],
    activityImg: `${cdnBase}/activity/banner.png`,
  }));
}

function mapDiscountItem(g, fallbackTitle) {
  const id = g.goodsId != null ? g.goodsId : g.id;
  const saleYuan = Number(
    g.seckillPrice != null
      ? g.seckillPrice
      : g.groupPrice != null
        ? g.groupPrice
        : g.salePrice || 0,
  );
  const originYuan = Number(g.originPrice != null ? g.originPrice : g.salePrice != null ? g.salePrice : saleYuan);
  const priceFen = Math.round(saleYuan * 100);
  const title = g.goodsName || g.activityName || fallbackTitle || '限时特惠';
  const priceLabel = formatPriceLabel(saleYuan);
  return {
    id: String(g.id != null ? g.id : id),
    spuId: String(id),
    title,
    thumb: resolveGoodsImage(g.goodsImg, id),
    price: priceFen,
    priceLabel,
    originPrice: Math.round(originYuan * 100),
    activityId: g.id,
  };
}

/** 用商品详情补齐秒杀活动缺失的名称/主图（兼容旧接口） */
async function enrichSeckillWithGoods(list) {
  const items = list || [];
  const needIds = [
    ...new Set(
      items
        .filter((g) => g.goodsId && (!g.goodsImg || !g.goodsName))
        .map((g) => g.goodsId),
    ),
  ];
  if (!needIds.length) {
    return items;
  }

  const pairs = await Promise.all(
    needIds.map(async (goodsId) => {
      try {
        const detail = await request.get(`/api/goods/goods/${goodsId}`);
        return [goodsId, detail];
      } catch (e) {
        return [goodsId, null];
      }
    }),
  );
  const map = Object.create(null);
  pairs.forEach(([goodsId, detail]) => {
    if (detail) map[goodsId] = detail;
  });

  return items.map((g) => {
    const goods = map[g.goodsId];
    if (!goods) return g;
    return {
      ...g,
      goodsName: g.goodsName || goods.goodsName,
      goodsImg: g.goodsImg || goods.goodsImg,
      originPrice: g.originPrice != null ? g.originPrice : goods.originPrice || goods.salePrice,
      salePrice: g.salePrice != null ? g.salePrice : goods.salePrice,
    };
  });
}

/** 获取首页数据：现时折扣横滑（最多10个秒杀）+ 一级大类 + Tab */
export async function fetchHome() {
  if (config.useMock) {
    return mockFetchHome();
  }

  let discountList = [];
  try {
    const seckill = (await request.get('/api/goods/seckill/list')) || [];
    const enriched = await enrichSeckillWithGoods(seckill);
    discountList = shuffleTake(enriched, DISCOUNT_LIMIT).map((g) => mapDiscountItem(g, '限时秒杀'));
  } catch (e) {
    console.warn('[Fresh] 秒杀折扣加载失败', e && e.message);
  }

  // 秒杀不足 10 个时，用库里热销商品补齐（仍展示真实商品图/价）
  if (discountList.length < DISCOUNT_LIMIT) {
    try {
      const hot = (await request.get('/api/goods/goods/hot')) || [];
      const used = new Set(discountList.map((d) => String(d.spuId)));
      const more = shuffleTake(
        hot.filter((g) => g && g.id != null && !used.has(String(g.id))),
        DISCOUNT_LIMIT - discountList.length,
      ).map((g) => {
        const card = adaptGoodsCard(g);
        const priceFen = (card && card.price) || yuanToFen(g.salePrice);
        return {
          id: `hot-${g.id}`,
          spuId: String(g.id),
          title: (card && card.title) || g.goodsName || '热销优选',
          thumb: (card && card.thumb) || resolveGoodsImage(g.goodsImg, g.id),
          price: priceFen,
          priceLabel: formatPriceLabel(priceFen / 100),
          originPrice: (card && card.originPrice) || yuanToFen(g.originPrice || g.salePrice),
        };
      });
      discountList = discountList.concat(more);
    } catch (e) {
      console.warn('[Fresh] 热销补齐失败', e && e.message);
    }
  }

  // 仍为空：直接用热销
  if (!discountList.length) {
    try {
      const hot = (await request.get('/api/goods/goods/hot')) || [];
      discountList = shuffleTake(hot, DISCOUNT_LIMIT).map((g) => {
        const card = adaptGoodsCard(g);
        const priceFen = (card && card.price) || yuanToFen(g.salePrice);
        return {
          id: String(g.id),
          spuId: String(g.id),
          title: (card && card.title) || g.goodsName || '热销优选',
          thumb: (card && card.thumb) || resolveGoodsImage(g.goodsImg, g.id),
          price: priceFen,
          priceLabel: formatPriceLabel(priceFen / 100),
          originPrice: (card && card.originPrice) || yuanToFen(g.originPrice || g.salePrice),
        };
      });
    } catch (e) {
      console.warn('[Fresh] 热销折扣加载失败', e && e.message);
    }
  }

  let categories = [];
  try {
    const tree = (await request.get('/api/goods/category/tree')) || [];
    categories = (tree || []).map((node) => {
      const iconRaw = (node.icon || '').trim();
      return {
        id: String(node.id),
        name: node.catName || node.name || '',
        icon:
          iconRaw && /^https?:\/\//i.test(iconRaw)
            ? resolveGoodsImage(iconRaw)
            : DEFAULT_CAT_ICON,
      };
    });
  } catch (e) {
    console.warn('[Fresh] 首页分类加载失败', e && e.message);
  }

  return {
    discountList,
    categories,
    tabList: [
      { text: '猜你喜欢', key: 0 },
      { text: '今日团购', key: 1 },
      { text: '限时秒杀', key: 2 },
    ],
    activityImg: (discountList[0] && discountList[0].thumb) || `${cdnBase}/activity/banner.png`,
  };
}
