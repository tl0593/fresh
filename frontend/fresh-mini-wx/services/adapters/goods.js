/**
 * Fresh 商品数据 ↔ 模板页面结构适配
 * 后端价格单位：元；模板展示单位：分
 */

/** 占位图：走腾讯 CDN，国内小程序更稳（Unsplash 常 404/被拦） */
const PLACEHOLDER_IMGS = [
  'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png',
  'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09b.png',
  'https://tdesign.gtimg.com/miniprogram/template/retail/goods/dz-3a.png',
  'https://tdesign.gtimg.com/miniprogram/template/retail/home/banner1.png',
];

const KNOWN_GOODS_IMG = {
  1: 'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png',
  2: 'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09b.png',
  3: 'https://tdesign.gtimg.com/miniprogram/template/retail/goods/dz-3a.png',
};

/**
 * 规范化商品图：
 * - 本地上传 http://127.0.0.1/.../upload/... 原样返回，由 webp-image 桥接展示
 * - 失效 Unsplash / 空地址 → 占位图
 */
export function resolveGoodsImage(url, goodsId) {
  const raw = (url || '').trim();
  if (
    !raw ||
    raw === 'null' ||
    raw === 'undefined' ||
    raw.indexOf('example.com') >= 0 ||
    raw.indexOf('placeholder') >= 0 ||
    raw.indexOf('images.unsplash.com') >= 0
  ) {
    if (goodsId != null && KNOWN_GOODS_IMG[goodsId]) {
      return KNOWN_GOODS_IMG[goodsId];
    }
    const idx = Math.abs(Number(goodsId) || 0) % PLACEHOLDER_IMGS.length;
    return PLACEHOLDER_IMGS[idx];
  }

  // 本地网关上传：保留，交给组件下载转本地临时文件
  if (/\/api\/goods\/upload\//i.test(raw)) {
    return raw;
  }

  if (/^https:\/\//i.test(raw)) {
    return raw;
  }

  if (goodsId != null && KNOWN_GOODS_IMG[goodsId]) {
    return KNOWN_GOODS_IMG[goodsId];
  }
  const idx = Math.abs(Number(goodsId) || 0) % PLACEHOLDER_IMGS.length;
  return PLACEHOLDER_IMGS[idx];
}

export function yuanToFen(yuan) {
  const n = Number(yuan);
  if (Number.isNaN(n)) return 0;
  return Math.round(n * 100);
}

export function fenToYuan(fen) {
  const n = Number(fen);
  if (Number.isNaN(n)) return 0;
  return Number((n / 100).toFixed(2));
}

/** 热销/列表卡片 */
export function adaptGoodsCard(goods) {
  if (!goods) return null;
  const priceFen = yuanToFen(goods.salePrice);
  const originFen = yuanToFen(goods.originPrice || goods.salePrice);
  const img = resolveGoodsImage(goods.goodsImg, goods.id);
  return {
    spuId: String(goods.id),
    thumb: img,
    primaryImage: img,
    title: goods.goodsName || '',
    price: priceFen,
    originPrice: originFen,
    minSalePrice: priceFen,
    maxSalePrice: priceFen,
    maxLinePrice: originFen,
    tags: [],
    spuTagList: [],
    catId: goods.catId,
    unit: goods.unit,
    soldNum: goods.saleCount || 0,
  };
}

const DEFAULT_CAT_ICON =
  'https://tdesign.gtimg.com/miniprogram/template/retail/category/category-default.png';
const DEFAULT_LEAF_ICON =
  'https://tdesign.gtimg.com/miniprogram/template/retail/classify/img-1.png';

function catIcon(url, isLeaf) {
  const raw = (url || '').trim();
  if (raw && /^https?:\/\//i.test(raw) && raw.indexOf('example.com') < 0) {
    return resolveGoodsImage(raw);
  }
  return isLeaf ? DEFAULT_LEAF_ICON : DEFAULT_CAT_ICON;
}

function sortCategoryNodes(nodes = []) {
  return [...(nodes || [])].sort((a, b) => {
    const sa = a.sort != null ? Number(a.sort) : Number.MAX_SAFE_INTEGER;
    const sb = b.sort != null ? Number(b.sort) : Number.MAX_SAFE_INTEGER;
    if (sa !== sb) return sa - sb;
    return Number(a.id || 0) - Number(b.id || 0);
  });
}

function hasChildren(node) {
  return !!(node && node.children && node.children.length);
}

/**
 * 分类树 → 模板结构
 * 模板 level=3：左栏一级；右侧「二级标题 + 三级格子」
 * 模板 level=2：左栏一级；右侧直接展示二级格子
 * 返回 { list, level }
 */
export function adaptCategoryTree(list = []) {
  const roots = sortCategoryNodes(list);
  const depth3 = roots.some((r) => (r.children || []).some((c) => hasChildren(c)));

  if (depth3) {
    const adapted = roots.map((node) => ({
      groupId: String(node.id),
      name: node.catName || '',
      thumbnail: catIcon(node.icon, false),
      sort: node.sort,
      children: sortCategoryNodes(node.children).map((mid) => ({
        groupId: String(mid.id),
        name: mid.catName || '',
        thumbnail: catIcon(mid.icon, false),
        sort: mid.sort,
        children: sortCategoryNodes(mid.children).map((leaf) => ({
          groupId: String(leaf.id),
          name: leaf.catName || '',
          thumbnail: catIcon(leaf.icon, true),
          sort: leaf.sort,
        })),
      })),
    }));
    return { list: adapted, level: 3 };
  }

  // 二级：左栏一级，右栏二级（不再包一层假的中间节点）
  const adapted = roots.map((node) => ({
    groupId: String(node.id),
    name: node.catName || '',
    thumbnail: catIcon(node.icon, false),
    sort: node.sort,
    children: sortCategoryNodes(node.children).map((leaf) => ({
      groupId: String(leaf.id),
      name: leaf.catName || '',
      thumbnail: catIcon(leaf.icon, true),
      sort: leaf.sort,
    })),
  }));
  return { list: adapted, level: 2 };
}

/** 是否为可展示的图片地址（排除普通文本描述） */
function isImageUrl(value) {
  const raw = (value || '').trim();
  if (!raw || raw.length > 800) return false;
  if (/^https?:\/\//i.test(raw)) {
    return /\.(png|jpe?g|webp|gif|bmp)(\?|$)/i.test(raw) || /\/api\/goods\/upload\//i.test(raw);
  }
  return /\/api\/goods\/upload\//i.test(raw);
}

/**
 * GoodsDetailVO → 模板 genGood 结构
 */
export function adaptGoodsDetail(detail) {
  if (!detail) return null;
  const specs = detail.specs || [];
  const primaryImage = resolveGoodsImage(detail.goodsImg, detail.id);
  const images = (detail.images || [])
    .map((img) => (typeof img === 'string' ? img : img.imgUrl || img.imageUrl || img.url))
    .filter(Boolean)
    .map((u) => resolveGoodsImage(u, detail.id));
  const imageList = images.length ? images : [primaryImage];
  const rawDesc = (detail.goodsDesc || '').trim();
  const descText = rawDesc && !isImageUrl(rawDesc) ? rawDesc : '';
  const descImages = rawDesc && isImageUrl(rawDesc) ? [resolveGoodsImage(rawDesc, detail.id)] : [];

  const prices = specs.length
    ? specs.map((s) => yuanToFen(s.specPrice))
    : [yuanToFen(detail.salePrice)];
  const minSalePrice = Math.min(...prices);
  const maxSalePrice = Math.max(...prices);
  const originFen = yuanToFen(detail.originPrice || detail.salePrice);

  const SPEC_ID = 'fresh_spec';
  const specList = [
    {
      specId: SPEC_ID,
      title: '规格',
      specValueList: (specs.length ? specs : [{ id: 0, specName: detail.unit || '份', stock: detail.totalStock }]).map(
        (s) => ({
          specValueId: String(s.id || 0),
          specId: SPEC_ID,
          saasId: null,
          specValue: s.specName || detail.unit || '默认',
          image: null,
        }),
      ),
    },
  ];

  const skuList = (specs.length ? specs : [{ id: 0, specName: detail.unit || '份', specPrice: detail.salePrice, stock: detail.totalStock }]).map(
    (s) => ({
      skuId: String(s.id || 0),
      skuImage: primaryImage,
      specInfo: [
        {
          specId: SPEC_ID,
          specTitle: '规格',
          specValueId: String(s.id || 0),
          specValue: s.specName || detail.unit || '默认',
        },
      ],
      priceInfo: [
        { priceType: 1, price: String(yuanToFen(s.specPrice != null ? s.specPrice : detail.salePrice)), priceTypeName: null },
        { priceType: 2, price: String(originFen), priceTypeName: null },
      ],
      stockInfo: {
        stockQuantity: s.stock != null ? s.stock : detail.totalStock || 0,
        safeStockQuantity: 0,
        soldQuantity: 0,
      },
      price: yuanToFen(s.specPrice != null ? s.specPrice : detail.salePrice),
      weight: { value: null, unit: detail.unit || '份' },
      volume: null,
      profitPrice: null,
    }),
  );

  return {
    saasId: 'fresh',
    storeId: '1',
    spuId: String(detail.id),
    title: detail.goodsName || '',
    primaryImage,
    images: imageList,
    video: null,
    available: 1,
    minSalePrice,
    minLinePrice: originFen,
    maxSalePrice,
    maxLinePrice: originFen,
    spuStockQuantity: detail.totalStock || 0,
    soldNum: detail.saleCount || 0,
    isPutOnSale: 1,
    categoryIds: [],
    specList,
    skuList,
    // 详情介绍：文字走 descText；仅真实图片 URL 进 desc，禁止用轮播图/占位图冒充详情
    intro: descText,
    descText,
    desc: descImages,
    hasDesc: !!(descText || descImages.length),
    unit: detail.unit,
    commentRate: detail.commentRate || null,
  };
}

/** 地址 VO → 模板地址结构（自提点） */
export function adaptAddress(addr) {
  if (!addr) return null;
  const community = addr.community || '';
  const detailAddr = addr.detailAddr || addr.detailAddress || '';
  return {
    id: addr.id,
    addressId: addr.id,
    name: addr.name || '',
    phone: addr.phone || '',
    phoneNumber: addr.phone || '',
    community,
    detailAddr,
    detailAddress: detailAddr,
    provinceName: community,
    cityName: '',
    districtName: community,
    address: `${community}${detailAddr}`,
    isDefault: addr.isDefault === 1 || addr.isDefault === true ? 1 : 0,
    tag: '自提点',
    addressTag: '自提点',
  };
}

/** 优惠券模板 → 模板卡片 */
export function adaptCouponTemplate(t) {
  if (!t) return null;
  const reduceFen = yuanToFen(t.reduceAmount);
  const fullFen = yuanToFen(t.fullAmount);
  return {
    key: String(t.id),
    id: t.id,
    status: 'default',
    type: 1, // ui-coupon-card 满减
    value: reduceFen,
    tag: '',
    desc: fullFen > 0 ? `满${Number(t.fullAmount)}元可用` : '无门槛使用',
    base: fullFen,
    title: t.couponName || '优惠券',
    timeLimit: t.endTime ? String(t.endTime).slice(0, 10) : '长期有效',
    currency: '¥',
    remainCount: t.remainCount,
  };
}

/** 用户已领券 → 卡片 */
export function adaptUserCoupon(c) {
  if (!c) return null;
  const reduceFen = yuanToFen(c.reduceAmount);
  const fullFen = yuanToFen(c.fullAmount);
  const useStatus = Number(c.useStatus);
  let status = 'default';
  if (useStatus === 1) status = 'useless';
  else if (useStatus === 2) status = 'disabled';
  return {
    key: String(c.id),
    id: c.id,
    templateId: c.templateId,
    status,
    type: 1,
    value: reduceFen,
    tag: '',
    desc: fullFen > 0 ? `满${Number(c.fullAmount)}元可用` : '无门槛使用',
    base: fullFen,
    title: c.couponName || '优惠券',
    timeLimit: c.validEnd ? String(c.validEnd).replace('T', ' ').slice(0, 16) : '长期有效',
    currency: '¥',
  };
}
