import Dialog from 'tdesign-miniprogram/dialog/index';
import Toast from 'tdesign-miniprogram/toast/index';
import { config } from '../../config/index';
import { fetchCartGroupData, updateCart } from '../../services/cart/cart';

Page({
  data: {
    cartGroupData: null,
    isManageMode: false,
  },

  onShow() {
    this.getTabBar().init();
    this.refreshData();
  },

  onLoad() {
    this.refreshData();
  },

  toggleManageMode() {
    this.setData({ isManageMode: !this.data.isManageMode });
  },

  refreshData() {
    this.getCartGroupData().then((res) => {
      let isEmpty = true;
      const cartGroupData = res.data;
      for (const store of cartGroupData.storeGoods) {
        store.isSelected = true;
        store.storeStockShortage = false;
        if (!store.shortageGoodsList) {
          store.shortageGoodsList = [];
        }
        for (const activity of store.promotionGoodsList) {
          activity.goodsPromotionList = activity.goodsPromotionList.filter((goods) => {
            goods.originPrice = undefined;
            if (goods.quantity > goods.stockQuantity) {
              store.storeStockShortage = true;
            }
            if (!goods.isSelected) {
              store.isSelected = false;
            }
            if (goods.stockQuantity > 0) {
              return true;
            }
            store.shortageGoodsList.push(goods);
            return false;
          });
          if (activity.goodsPromotionList.length > 0) {
            isEmpty = false;
          }
        }
        if (store.shortageGoodsList.length > 0) {
          isEmpty = false;
        }
      }
      cartGroupData.invalidGoodItems = (cartGroupData.invalidGoodItems || []).map((goods) => {
        goods.originPrice = undefined;
        return goods;
      });
      cartGroupData.isNotEmpty = !isEmpty;
      this.setData({ cartGroupData });
    });
  },

  findGoods(spuId, skuId) {
    let currentStore;
    let currentActivity;
    let currentGoods;
    const { storeGoods } = this.data.cartGroupData;
    for (const store of storeGoods) {
      for (const activity of store.promotionGoodsList) {
        for (const goods of activity.goodsPromotionList) {
          if (String(goods.spuId) === String(spuId) && String(goods.skuId) === String(skuId)) {
            currentStore = store;
            currentActivity = activity;
            currentGoods = goods;
            return { currentStore, currentActivity, currentGoods };
          }
        }
      }
    }
    return { currentStore, currentActivity, currentGoods };
  },

  getCartGroupData() {
    return fetchCartGroupData();
  },

  selectGoodsService({ spuId, skuId, isSelected }) {
    if (!config.useMock) {
      return updateCart({
        goodsId: spuId,
        specId: skuId,
        selected: isSelected ? 1 : 0,
      });
    }
    this.findGoods(spuId, skuId).currentGoods.isSelected = isSelected;
    return Promise.resolve();
  },

  selectStoreService({ storeId, isSelected }) {
    const currentStore = this.data.cartGroupData.storeGoods.find((s) => s.storeId === storeId);
    const tasks = [];
    currentStore.isSelected = isSelected;
    currentStore.promotionGoodsList.forEach((activity) => {
      activity.goodsPromotionList.forEach((goods) => {
        goods.isSelected = isSelected;
        if (!config.useMock) {
          tasks.push(
            updateCart({
              goodsId: goods.spuId,
              specId: goods.skuId,
              selected: isSelected ? 1 : 0,
            }),
          );
        }
      });
    });
    return tasks.length ? Promise.all(tasks) : Promise.resolve();
  },

  changeQuantityService({ spuId, skuId, quantity }) {
    if (!config.useMock) {
      return updateCart({ goodsId: spuId, specId: skuId, num: quantity });
    }
    this.findGoods(spuId, skuId).currentGoods.quantity = quantity;
    return Promise.resolve();
  },

  deleteGoodsService({ spuId, skuId }) {
    if (!config.useMock) {
      return updateCart({ goodsId: spuId, specId: skuId, num: 0 });
    }
    function deleteGoods(group) {
      for (const gindex in group) {
        const goods = group[gindex];
        if (String(goods.spuId) === String(spuId) && String(goods.skuId) === String(skuId)) {
          group.splice(gindex, 1);
          return gindex;
        }
      }
      return -1;
    }
    const { storeGoods, invalidGoodItems } = this.data.cartGroupData;
    for (const store of storeGoods) {
      for (const activity of store.promotionGoodsList) {
        if (deleteGoods(activity.goodsPromotionList) > -1) {
          return Promise.resolve();
        }
      }
      if (deleteGoods(store.shortageGoodsList) > -1) {
        return Promise.resolve();
      }
    }
    if (deleteGoods(invalidGoodItems) > -1) {
      return Promise.resolve();
    }
    return Promise.reject();
  },

  clearInvalidGoodsService() {
    this.data.cartGroupData.invalidGoodItems = [];
    return Promise.resolve();
  },

  onGoodsSelect(e) {
    const {
      goods: { spuId, skuId },
      isSelected,
    } = e.detail;
    const { currentGoods } = this.findGoods(spuId, skuId);
    Toast({
      context: this,
      selector: '#t-toast',
      message: `${isSelected ? '选择' : '取消'}"${
        currentGoods.title.length > 5 ? `${currentGoods.title.slice(0, 5)}...` : currentGoods.title
      }"`,
      icon: '',
    });
    this.selectGoodsService({ spuId, skuId, isSelected }).then(() => this.refreshData());
  },

  onStoreSelect(e) {
    const {
      store: { storeId },
      isSelected,
    } = e.detail;
    this.selectStoreService({ storeId, isSelected }).then(() => this.refreshData());
  },

  onQuantityChange(e) {
    const {
      goods: { spuId, skuId },
      quantity,
    } = e.detail;
    const { currentGoods } = this.findGoods(spuId, skuId);
    const stockQuantity = currentGoods.stockQuantity > 0 ? currentGoods.stockQuantity : 0;
    if (quantity > stockQuantity) {
      if (currentGoods.quantity === stockQuantity && quantity - stockQuantity === 1) {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '当前商品库存不足',
        });
        return;
      }
      Dialog.confirm({
        title: '商品库存不足',
        content: `当前商品库存不足，最大可购买数量为${stockQuantity}件`,
        confirmBtn: '修改为最大可购买数量',
        cancelBtn: '取消',
      })
        .then(() => {
          this.changeQuantityService({ spuId, skuId, quantity: stockQuantity }).then(() =>
            this.refreshData(),
          );
        })
        .catch(() => {});
      return;
    }
    this.changeQuantityService({ spuId, skuId, quantity }).then(() => this.refreshData());
  },

  goCollect() {
    // 购物车不再跳转独立团购页，回首页团购分类
    wx.switchTab({ url: '/pages/home/home' });
  },

  goGoodsDetail(e) {
    const { spuId, storeId } = e.detail.goods;
    wx.navigateTo({
      url: `/pages/goods/details/index?spuId=${spuId}&storeId=${storeId}`,
    });
  },

  clearInvalidGoods() {
    this.clearInvalidGoodsService().then(() => this.refreshData());
  },

  onGoodsDelete(e) {
    const goods = (e.detail && e.detail.goods) || {};
    const spuId = goods.spuId;
    const skuId = goods.skuId;
    if (!spuId || !skuId) {
      Toast({ context: this, selector: '#t-toast', message: '商品信息缺失' });
      return;
    }
    Dialog.confirm({
      content: '确认删除该商品吗?',
      confirmBtn: '确定',
      cancelBtn: '取消',
    })
      .then(() =>
        this.deleteGoodsService({ spuId, skuId }).then(() => {
          Toast({ context: this, selector: '#t-toast', message: '商品删除成功' });
          this.refreshData();
        }),
      )
      .catch((err) => {
        if (err && err.message) {
          Toast({ context: this, selector: '#t-toast', message: err.message || '删除失败' });
        }
      });
  },

  /** 管理态：删除勾选的商品 */
  onDeleteSelected() {
    const selected = [];
    const { cartGroupData } = this.data;
    if (!cartGroupData || !cartGroupData.storeGoods) return;
    cartGroupData.storeGoods.forEach((store) => {
      (store.promotionGoodsList || []).forEach((promotion) => {
        (promotion.goodsPromotionList || []).forEach((g) => {
          if (g.isSelected == 1) {
            selected.push({ spuId: g.spuId, skuId: g.skuId });
          }
        });
      });
    });
    if (!selected.length) {
      Toast({ context: this, selector: '#t-toast', message: '请先勾选商品' });
      return;
    }
    Dialog.confirm({
      content: `确认删除已选的 ${selected.length} 件商品吗?`,
      confirmBtn: '确定',
      cancelBtn: '取消',
    })
      .then(() =>
        Promise.all(selected.map((item) => this.deleteGoodsService(item))).then(() => {
          Toast({ context: this, selector: '#t-toast', message: '删除成功' });
          this.setData({ isManageMode: false });
          this.refreshData();
        }),
      )
      .catch((err) => {
        if (err && err.message) {
          Toast({ context: this, selector: '#t-toast', message: err.message || '删除失败' });
        }
      });
  },

  onSelectAll(event) {
    const { isAllSelected } = event?.detail ?? {};
    const { storeGoods } = this.data.cartGroupData;
    const nextSelected = !isAllSelected;
    Promise.all(
      storeGoods.map((store) =>
        this.selectStoreService({ storeId: store.storeId, isSelected: nextSelected }),
      ),
    ).then(() => this.refreshData());
  },

  onToSettle() {
    const goodsRequestList = [];
    this.data.cartGroupData.storeGoods.forEach((store) => {
      store.promotionGoodsList.forEach((promotion) => {
        promotion.goodsPromotionList.forEach((m) => {
          if (m.isSelected == 1) {
            goodsRequestList.push({
              ...m,
              goodsId: m.spuId,
              specId: m.skuId,
              goodsName: m.title,
              num: m.quantity,
            });
          }
        });
      });
    });
    wx.setStorageSync('order.goodsRequestList', JSON.stringify(goodsRequestList));
    wx.navigateTo({ url: '/pages/order/order-confirm/index?type=cart' });
  },

  onGotoHome() {
    wx.switchTab({ url: '/pages/home/home' });
  },
});
