import Toast from 'tdesign-miniprogram/toast/index';
import request from '../../utils/request';
import { fetchGoodsList } from '../../services/good/fetchGoodsList';
import { addToCart } from '../../services/cart/cart';
import { resolveGoodsImage } from '../../services/adapters/goods';

const DEFAULT_ICON =
  'https://tdesign.gtimg.com/miniprogram/template/retail/category/category-default.png';

function mapNode(node) {
  const iconRaw = (node.icon || '').trim();
  return {
    id: String(node.id),
    name: node.catName || node.name || '',
    icon: iconRaw && /^https?:\/\//i.test(iconRaw) ? resolveGoodsImage(iconRaw) : DEFAULT_ICON,
    children: (node.children || []).map(mapNode),
  };
}

Page({
  data: {
    keyword: '',
    roots: [],
    rootIndex: 0,
    sideList: [],
    sideIndex: 0,
    currentCatId: '',
    sortType: 'sale',
    sortOrder: 'desc',
    goodsList: [],
    loading: false,
    hasLoaded: false,
    loadMoreStatus: 0,
  },

  pageNum: 1,
  pageSize: 20,
  total: 0,

  onShow() {
    const tab = this.getTabBar && this.getTabBar();
    if (tab && tab.init) tab.init();
    if (this.data.roots.length) {
      this.applyHomeSelectedCat();
    }
  },

  onLoad() {
    this.initCategories();
  },

  /** 首页大类跳转：选中对应一级分类；成功则返回 true */
  applyHomeSelectedCat() {
    let catId = '';
    try {
      catId = String(wx.getStorageSync('homeSelectedCatId') || '');
      if (!catId) return false;
      wx.removeStorageSync('homeSelectedCatId');
      wx.removeStorageSync('homeSelectedCatName');
    } catch (e) {
      return false;
    }
    if (!this.data.roots.length) return false;
    const { roots } = this.data;
    const rootIndex = roots.findIndex((r) => String(r.id) === catId);
    if (rootIndex < 0) return false;
    const root = roots[rootIndex];
    const sideList = this.buildSideList(root);
    this.setData(
      {
        rootIndex,
        sideList,
        sideIndex: 0,
        currentCatId: sideList[0] ? sideList[0].id : root.id,
        keyword: '',
      },
      () => this.loadGoods(true),
    );
    return true;
  },

  async initCategories() {
    try {
      const tree = (await request.get('/api/goods/category/tree')) || [];
      const roots = (tree || []).map(mapNode);
      if (!roots.length) {
        this.setData({ roots: [], sideList: [], hasLoaded: true });
        return;
      }
      const sideList = this.buildSideList(roots[0]);
      this.setData(
        {
          roots,
          rootIndex: 0,
          sideList,
          sideIndex: 0,
          currentCatId: sideList[0] ? sideList[0].id : roots[0].id,
        },
        () => {
          if (!this.applyHomeSelectedCat()) {
            this.loadGoods(true);
          }
        },
      );
    } catch (e) {
      console.error('分类加载失败', e);
      Toast({ context: this, selector: '#t-toast', message: '分类加载失败' });
    }
  },

  /** 左侧：全部 + 子分类；无子级时仅「全部」 */
  buildSideList(root) {
    const children = (root && root.children) || [];
    const list = [{ id: root.id, name: '全部' }];
    children.forEach((c) => list.push({ id: c.id, name: c.name }));
    return list;
  },

  onRootTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    const { roots } = this.data;
    const root = roots[index];
    if (!root) return;
    const sideList = this.buildSideList(root);
    this.setData(
      {
        rootIndex: index,
        sideList,
        sideIndex: 0,
        currentCatId: sideList[0].id,
        keyword: '',
      },
      () => this.loadGoods(true),
    );
  },

  onSideTap(e) {
    const index = Number(e.currentTarget.dataset.index);
    const item = this.data.sideList[index];
    if (!item) return;
    this.setData(
      {
        sideIndex: index,
        currentCatId: item.id,
      },
      () => this.loadGoods(true),
    );
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value || '' });
  },

  onSearch() {
    this.loadGoods(true);
  },

  onSortTap(e) {
    const type = e.currentTarget.dataset.type;
    let { sortType, sortOrder } = this.data;
    if (type === 'sale') {
      sortType = 'sale';
      sortOrder = 'desc';
    } else if (type === 'price') {
      if (sortType === 'price') {
        sortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
      } else {
        sortType = 'price';
        sortOrder = 'asc';
      }
    }
    this.setData({ sortType, sortOrder }, () => this.loadGoods(true));
  },

  async loadGoods(reset = false) {
    if (this.data.loading) return;
    if (!reset && this.data.loadMoreStatus === 2) return;

    const pageNum = reset ? 1 : this.pageNum + 1;
    this.setData({ loading: true, loadMoreStatus: reset ? 1 : 1 });

    try {
      const { currentCatId, keyword, sortType, sortOrder, goodsList } = this.data;
      const result = await fetchGoodsList({
        categoryId: currentCatId,
        keyword,
        sortType,
        sortOrder,
        pageNum,
        pageSize: this.pageSize,
      });
      const list = result.spuList || [];
      const total = result.totalCount || 0;
      const nextList = reset ? list : goodsList.concat(list);
      this.pageNum = pageNum;
      this.total = total;
      this.setData({
        goodsList: nextList,
        hasLoaded: true,
        loading: false,
        loadMoreStatus: nextList.length >= total ? 2 : 0,
      });
    } catch (e) {
      console.error('商品加载失败', e);
      this.setData({ loading: false, hasLoaded: true, loadMoreStatus: 0 });
      Toast({ context: this, selector: '#t-toast', message: '商品加载失败' });
    }
  },

  onReachGoodsBottom() {
    if (this.data.loadMoreStatus !== 0 || this.data.loading) return;
    this.loadGoods(false);
  },

  gotoDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/goods/details/index?spuId=${id}` });
  },

  async onAddCart(e) {
    const goodsId = e.currentTarget.dataset.id;
    if (!goodsId) return;
    try {
      const detail = await request.get(`/api/goods/goods/${goodsId}`);
      const specs = (detail && detail.specs) || [];
      const spec = specs.find((s) => s.isDefault === 1) || specs[0];
      if (!spec) {
        Toast({ context: this, selector: '#t-toast', message: '暂无规格可加购' });
        return;
      }
      await addToCart({
        goodsId,
        specId: spec.id,
        num: 1,
        selected: 1,
      });
      Toast({ context: this, selector: '#t-toast', message: '已加入购物车' });
    } catch (err) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: (err && err.message) || '加购失败',
      });
    }
  },
});
