import { config } from '../../config/index';
import request from '../../utils/request';
import { ensureLogin } from '../auth/login';
import { yuanToFen, resolveGoodsImage } from '../adapters/goods';

const goodsCache = {};

async function fetchGoodsDetailCached(goodsId) {
  const key = String(goodsId);
  if (goodsCache[key]) return goodsCache[key];
  const detail = await request.get(`/api/goods/goods/${goodsId}`);
  goodsCache[key] = detail;
  return detail;
}

function findSpec(detail, specId) {
  const specs = (detail && detail.specs) || [];
  if (!specs.length) return null;
  const found = specs.find((s) => String(s.id) === String(specId));
  return found || specs.find((s) => s.isDefault === 1) || specs[0];
}

/** 修复历史写入的 UTF-8 乱码规格名（如 é»˜è®¤ → 默认） */
function normalizeSpecName(name) {
  let s = String(name || '').trim();
  if (!s) return '默认规格';
  if (s.indexOf('é»˜è®¤') >= 0) {
    s = s.split('é»˜è®¤').join('默认');
  }
  return s;
}

/** 获取购物车 mock 数据 */
function mockFetchCartGroupData(params) {
  const { delay } = require('../_utils/delay');
  const { genCartGroupData } = require('../../model/cart');
  return delay().then(() => genCartGroupData(params));
}

/** 组装模板购物车结构 */
function buildCartGroupData(items, detailsMap) {
  const goodsPromotionList = [];
  for (const item of items || []) {
    const detail = detailsMap[String(item.goodsId)];
    if (!detail) continue;
    const spec = findSpec(detail, item.specId);
    const priceYuan = spec ? spec.specPrice : detail.salePrice;
    const stock = spec ? spec.stock : detail.totalStock;
    const img = resolveGoodsImage(detail.goodsImg, item.goodsId);
    const priceFen = yuanToFen(priceYuan);
    goodsPromotionList.push({
      uid: `${item.goodsId}_${item.specId}`,
      saasId: 'fresh',
      storeId: '1',
      spuId: String(item.goodsId),
      skuId: String(item.specId),
      isSelected: item.selected === 1 ? 1 : 0,
      thumb: img,
      title: detail.goodsName || '',
      primaryImage: img,
      quantity: item.num || 1,
      stockStatus: true,
      stockQuantity: stock != null ? stock : 99,
      price: String(priceFen),
      originPrice: String(yuanToFen(detail.originPrice || priceYuan)),
      tagPrice: null,
      titlePrefixTags: null,
      roomId: null,
      specInfo: [
        {
          specTitle: '规格',
          specValue: normalizeSpecName((spec && spec.specName) || detail.unit || '默认规格'),
        },
      ],
      available: 1,
      putOnSale: 1,
      etitle: null,
    });
  }

  const selected = goodsPromotionList.filter((g) => g.isSelected === 1);
  const totalAmount = String(
    selected.reduce((sum, g) => sum + Number(g.price || 0) * Number(g.quantity || 0), 0),
  );

  return {
    data: {
      isNotEmpty: goodsPromotionList.length > 0,
      storeGoods: [
        {
          storeId: '1',
          storeName: '社区生鲜自提点',
          storeStatus: 1,
          totalDiscountSalePrice: '0',
          promotionGoodsList: [
            {
              promotionCode: 'EMPTY_PROMOTION',
              promotionSubCode: 'NORMAL',
              promotionId: '0',
              tagText: [],
              promotionStatus: 3,
              tag: '',
              description: '',
              doorSillRemain: null,
              isNeedAddOnShop: 0,
              goodsPromotionList,
            },
          ],
          shortageGoodsList: [],
        },
      ],
      invalidGoodItems: [],
      isAllSelected: selected.length > 0 && selected.length === goodsPromotionList.length,
      selectedGoodsCount: selected.length,
      totalAmount,
      totalDiscountAmount: '0',
    },
  };
}

/** 获取购物车数据 */
export async function fetchCartGroupData() {
  if (config.useMock) {
    return mockFetchCartGroupData();
  }

  await ensureLogin();
  const items = (await request.get('/api/user/cart/list')) || [];
  const detailsMap = {};
  await Promise.all(
    items.map(async (item) => {
      try {
        detailsMap[String(item.goodsId)] = await fetchGoodsDetailCached(item.goodsId);
      } catch (e) {
        console.warn('[Fresh] 购物车商品详情失败', item.goodsId, e && e.message);
      }
    }),
  );
  return buildCartGroupData(items, detailsMap);
}

/** 更新购物车（改数量/选中/删除 num<=0；覆盖数量） */
export async function updateCart({ goodsId, specId, num, selected, increment }) {
  await ensureLogin();
  const gid = Number(goodsId);
  const sid = Number(specId);
  if (!Number.isFinite(gid) || gid <= 0) {
    throw new Error('商品信息无效');
  }
  if (!Number.isFinite(sid) || sid < 0) {
    throw new Error('请选择规格');
  }
  const body = {
    goodsId: gid,
    specId: sid,
  };
  if (num != null) body.num = Number(num);
  if (selected != null) body.selected = Number(selected);
  if (increment != null) body.increment = !!increment;
  return request.post('/api/user/cart/update', body);
}

/** 加购：已有同规格则累加数量 */
export async function addToCart({ goodsId, specId, num = 1, selected = 1 }) {
  return updateCart({
    goodsId,
    specId,
    num: Number(num) > 0 ? Number(num) : 1,
    selected: selected != null ? selected : 1,
    increment: true,
  });
}

/** 下单成功后移除购物车中对应商品 */
export async function removeCartItems(goodsList) {
  const list = goodsList || [];
  if (!list.length) return;
  await ensureLogin();
  await Promise.all(
    list.map((g) => {
      const goodsId = Number(g.goodsId || g.spuId);
      const specId = Number(g.specId || g.skuId);
      if (!Number.isFinite(goodsId) || goodsId <= 0 || !Number.isFinite(specId) || specId < 0) {
        return Promise.resolve();
      }
      return updateCart({ goodsId, specId, num: 0 }).catch((e) => {
        console.warn('[Fresh] 结算后清购物车失败', goodsId, specId, e && e.message);
      });
    }),
  );
}
