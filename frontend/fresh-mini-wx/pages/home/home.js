import { fetchHome } from '../../services/home/home';
import { fetchGoodsList } from '../../services/good/fetchGoods';
import { addToCart } from '../../services/cart/cart';
import request from '../../utils/request';
import Toast from 'tdesign-miniprogram/toast/index';

Page({
  data: {
    discountList: [],
    categories: [],
    tabList: [],
    goodsList: [],
    goodsListLoadStatus: 0,
    pageLoading: false,
    flyBalls: [],
  },

  goodListPagination: {
    index: 0,
    num: 20,
  },

  privateData: {
    tabIndex: 0,
  },

  onShow() {
    this.getTabBar().init();
  },

  onLoad() {
    this.init();
  },

  onReachBottom() {
    if (this.data.goodsListLoadStatus === 0) {
      this.loadGoodsList();
    }
  },

  onPullDownRefresh() {
    this.init();
  },

  init() {
    this.loadHomePage();
  },

  loadHomePage() {
    wx.stopPullDownRefresh();
    this.setData({ pageLoading: true });
    fetchHome()
      .then(({ discountList, categories, tabList }) => {
        this.setData({
          discountList: discountList || [],
          categories: categories || [],
          tabList,
          pageLoading: false,
        });
        this.loadGoodsList(true);
      })
      .catch((e) => {
        console.error('首页加载失败', e);
        this.setData({ pageLoading: false });
        Toast({ context: this, selector: '#t-toast', message: '首页加载失败' });
      });
  },

  onDiscountTap(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/goods/details/index?spuId=${id}` });
  },

  navToSeckill() {
    wx.navigateTo({ url: '/pages/fresh/seckill/index' });
  },

  onCategoryTap(e) {
    const { id, name } = e.currentTarget.dataset;
    if (id) {
      try {
        wx.setStorageSync('homeSelectedCatId', String(id));
        wx.setStorageSync('homeSelectedCatName', name || '');
      } catch (err) {
        /* ignore */
      }
    }
    wx.switchTab({ url: '/pages/category/index' });
  },

  tabChangeHandle(e) {
    this.privateData.tabIndex = e.detail.value != null ? e.detail.value : e.detail;
    this.loadGoodsList(true);
  },

  onReTry() {
    this.loadGoodsList();
  },

  async loadGoodsList(fresh = false) {
    if (fresh) {
      wx.pageScrollTo({ scrollTop: 0 });
    }

    this.setData({ goodsListLoadStatus: 1 });

    const pageSize = this.goodListPagination.num;
    let pageIndex = this.privateData.tabIndex * pageSize + this.goodListPagination.index + 1;
    if (fresh) {
      pageIndex = 0;
    }

    try {
      const tabKey = this.privateData.tabIndex || 0;
      const nextList = await fetchGoodsList(pageIndex, pageSize, tabKey);
      this.setData({
        goodsList: fresh ? nextList : this.data.goodsList.concat(nextList),
        goodsListLoadStatus: nextList.length < pageSize ? 2 : 0,
      });
      this.goodListPagination.index = pageIndex;
      this.goodListPagination.num = pageSize;
    } catch (err) {
      this.setData({ goodsListLoadStatus: 3 });
    }
  },

  goodListClickHandle(e) {
    const { index } = e.detail;
    const item = this.data.goodsList[index];
    if (!item || !item.spuId) return;
    wx.navigateTo({
      url: `/pages/goods/details/index?spuId=${item.spuId}`,
    });
  },

  getCartTabPoint() {
    const sys = wx.getSystemInfoSync();
    const bottomGap = (sys.safeAreaInsets && sys.safeAreaInsets.bottom) || 0;
    // 4 个 Tab，购物车为第 3 个（index=2）中心点
    return {
      x: (sys.windowWidth * 2.5) / 4 - 14,
      y: sys.windowHeight - bottomGap - 52,
    };
  },

  playFlyToCart({ clientX, clientY, thumb }) {
    const startX = Number(clientX);
    const startY = Number(clientY);
    if (!Number.isFinite(startX) || !Number.isFinite(startY)) return;
    const end = this.getCartTabPoint();
    const id = `fly_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const ball = {
      id,
      x: startX - 14,
      y: startY - 14,
      thumb: thumb || '',
      flying: false,
    };
    this.setData({ flyBalls: (this.data.flyBalls || []).concat(ball) });
    const startFly = () => {
      const list = (this.data.flyBalls || []).map((b) =>
        b.id === id ? { ...b, x: end.x, y: end.y, flying: true } : b,
      );
      this.setData({ flyBalls: list });
    };
    if (typeof wx.nextTick === 'function') wx.nextTick(startFly);
    else setTimeout(startFly, 30);
    setTimeout(() => {
      this.setData({
        flyBalls: (this.data.flyBalls || []).filter((b) => b.id !== id),
      });
      const tabBar = typeof this.getTabBar === 'function' ? this.getTabBar() : null;
      if (tabBar && typeof tabBar.bumpCart === 'function') {
        tabBar.bumpCart(1);
      }
    }, 720);
  },

  async goodListAddCartHandle(e) {
    const detail = e.detail || {};
    const { index, goods, clientX, clientY } = detail;
    const item = goods || this.data.goodsList[index];
    const goodsId = item && (item.spuId || item.id);
    if (!goodsId) {
      Toast({ context: this, selector: '#t-toast', message: '商品信息缺失' });
      return;
    }

    // 团购/秒杀活动卡：跳详情，不直接加购
    if (item.activityType === 2 || item.activityType === 3) {
      wx.navigateTo({ url: `/pages/goods/details/index?spuId=${goodsId}` });
      return;
    }

    try {
      const goodsDetail = await request.get(`/api/goods/goods/${goodsId}`);
      const specs = (goodsDetail && goodsDetail.specs) || [];
      const spec = specs.find((s) => s.isDefault === 1) || specs[0];
      if (!spec) {
        Toast({ context: this, selector: '#t-toast', message: '暂无规格可加购' });
        return;
      }
      // 先播动画，接口并行，体感更跟手
      this.playFlyToCart({
        clientX,
        clientY,
        thumb: (item && (item.thumb || item.primaryImage)) || goodsDetail.goodsImg || '',
      });
      await addToCart({
        goodsId,
        specId: spec.id,
        num: 1,
        selected: 1,
      });
    } catch (err) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: (err && err.message) || '加购失败',
      });
    }
  },

  navToSearchPage() {
    wx.navigateTo({ url: '/pages/goods/search/index' });
  },
});
