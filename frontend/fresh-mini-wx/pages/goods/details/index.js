import Toast from 'tdesign-miniprogram/toast/index';
import { fetchGood } from '../../../services/good/fetchGood';
import { fetchActivityList } from '../../../services/activity/fetchActivityList';
import {
  getGoodsDetailsCommentList,
  getGoodsDetailsCommentsCount,
} from '../../../services/good/fetchGoodsDetailsComments';
import { addToCart } from '../../../services/cart/cart';
import { config, cdnBase } from '../../../config/index';

const imgPrefix = `${cdnBase}/`;

const recLeftImg = `${imgPrefix}common/rec-left.png`;
const recRightImg = `${imgPrefix}common/rec-right.png`;
const obj2Params = (obj = {}, encode = false) => {
  const result = [];
  Object.keys(obj).forEach((key) => result.push(`${key}=${encode ? encodeURIComponent(obj[key]) : obj[key]}`));

  return result.join('&');
};

Page({
  data: {
    commentsList: [],
    commentsStatistics: {
      badCount: 0,
      commentCount: 0,
      goodCount: 0,
      goodRate: 0,
      hasImageCount: 0,
      middleCount: 0,
    },
    isShowPromotionPop: false,
    activityList: [],
    recLeftImg,
    recRightImg,
    details: { desc: [], descText: '', hasDesc: false },
    intro: '',
    anonymityAvatar: 'https://tdesign.gtimg.com/miniprogram/template/retail/user/avatar.png',
    goodsTabArray: [
      {
        name: '商品',
        value: '', // 空字符串代表置顶
      },
      {
        name: '详情',
        value: 'goods-page',
      },
    ],
    storeLogo: `${imgPrefix}common/store-logo.png`,
    storeName: '社区生鲜自提点',
    jumpArray: [
      {
        title: '首页',
        url: '/pages/home/home',
        iconName: 'home',
      },
      {
        title: '购物车',
        url: '/pages/cart/index',
        iconName: 'cart',
        showCartNum: true,
      },
    ],
    isStock: true,
    cartNum: 0,
    soldout: false,
    buttonType: 1,
    buyNum: 1,
    limitMaxCount: 999,
    selectedAttrStr: '',
    skuArray: [],
    primaryImage: '',
    specImg: '',
    isSpuSelectPopupShow: false,
    isAllSelectedSku: false,
    buyType: 0,
    outOperateStatus: false, // 是否外层加入购物车
    operateType: 0,
    selectSkuSellsPrice: 0,
    maxLinePrice: 0,
    minSalePrice: 0,
    maxSalePrice: 0,
    list: [],
    spuId: '',
    navigation: { type: 'fraction' },
    current: 0,
    autoplay: true,
    duration: 500,
    interval: 5000,
    soldNum: 0, // 已售数量
  },

  handlePopupHide() {
    this.setData({
      isSpuSelectPopupShow: false,
    });
  },

  showSkuSelectPopup(type) {
    this.setData({
      buyType: type || 0,
      outOperateStatus: type >= 1,
      isSpuSelectPopupShow: true,
    });
  },

  buyItNow() {
    this.showSkuSelectPopup(1);
  },

  toAddCart() {
    this.showSkuSelectPopup(2);
  },

  toNav(e) {
    const { url } = e.detail;
    wx.switchTab({
      url: url,
    });
  },

  showCurImg(e) {
    const { index } = e.detail;
    const { images } = this.data.details;
    wx.previewImage({
      current: images[index],
      urls: images,
    });
  },

  onSwiperChange(e) {
    this.setData({ current: e.detail.current || 0 });
  },

  async gotoGoodsPremiere(e) {
    const index = Number(e.currentTarget.dataset.index || 0);
    const images = (this.data.details && this.data.details.images) || [];
    if (!images.length) return;
    try {
      const { mapDisplayableImages } = require('../../../utils/localImage');
      const urls = (await mapDisplayableImages(images)).filter(Boolean);
      if (!urls.length) return;
      wx.previewImage({
        current: urls[Math.min(index, urls.length - 1)],
        urls,
      });
    } catch (err) {
      wx.previewImage({ current: images[index], urls: images });
    }
  },

  onPageScroll({ scrollTop }) {
    const goodsTab = this.selectComponent('#goodsTab');
    goodsTab && goodsTab.onScroll(scrollTop);
  },

  chooseSpecItem(e) {
    const { specList } = this.data.details;
    const { selectedSku, isAllSelectedSku } = e.detail;
    if (!isAllSelectedSku) {
      this.setData({
        selectSkuSellsPrice: 0,
      });
    }
    this.setData({
      isAllSelectedSku,
    });
    this.getSkuItem(specList, selectedSku);
  },

  getSkuItem(specList, selectedSku) {
    const { skuArray, primaryImage } = this.data;
    const selectedSkuValues = this.getSelectedSkuValues(specList, selectedSku);
    let selectedAttrStr = ` 件  `;
    selectedSkuValues.forEach((item) => {
      selectedAttrStr += `，${item.specValue}  `;
    });
    // eslint-disable-next-line array-callback-return
    const skuItem = skuArray.filter((item) => {
      let status = true;
      (item.specInfo || []).forEach((subItem) => {
        if (!selectedSku[subItem.specId] || selectedSku[subItem.specId] !== subItem.specValueId) {
          status = false;
        }
      });
      if (status) return item;
    });
    this.selectSpecsName(selectedSkuValues.length > 0 ? selectedAttrStr : '');
    if (skuItem && skuItem.length) {
      const selected = { ...skuItem[0], price: skuItem[0].price || 0 };
      // attach price from skuList in details if needed
      const fullSku = (this.data.details.skuList || []).find((s) => String(s.skuId) === String(selected.skuId));
      if (fullSku) {
        selected.price = fullSku.price || Number((fullSku.priceInfo || [])[0]?.price) || 0;
        selected.skuImage = fullSku.skuImage;
      }
      this.setData({
        selectItem: selected,
        selectSkuSellsPrice: selected.price || 0,
      });
    } else {
      this.setData({
        selectItem: null,
        selectSkuSellsPrice: 0,
      });
    }
    this.setData({
      specImg: this.data.selectItem && this.data.selectItem.skuImage ? this.data.selectItem.skuImage : primaryImage,
    });
  },

  // 获取已选择的sku名称
  getSelectedSkuValues(skuTree, selectedSku) {
    const normalizedTree = this.normalizeSkuTree(skuTree);
    return Object.keys(selectedSku).reduce((selectedValues, skuKeyStr) => {
      const skuValues = normalizedTree[skuKeyStr];
      const skuValueId = selectedSku[skuKeyStr];
      if (skuValueId !== '') {
        const skuValue = skuValues.filter((value) => {
          return value.specValueId === skuValueId;
        })[0];
        skuValue && selectedValues.push(skuValue);
      }
      return selectedValues;
    }, []);
  },

  normalizeSkuTree(skuTree) {
    const normalizedTree = {};
    skuTree.forEach((treeItem) => {
      normalizedTree[treeItem.specId] = treeItem.specValueList;
    });
    return normalizedTree;
  },

  selectSpecsName(selectSpecsName) {
    if (selectSpecsName) {
      this.setData({
        selectedAttrStr: selectSpecsName,
      });
    } else {
      this.setData({
        selectedAttrStr: '',
      });
    }
  },

  getSelectedSku() {
    const { selectItem, details, skuArray } = this.data;
    if (Array.isArray(selectItem) && selectItem.length) {
      return selectItem[0];
    }
    if (selectItem && selectItem.skuId) {
      return selectItem;
    }
    return (skuArray && skuArray[0]) || (details.skuList && details.skuList[0]) || null;
  },

  async addCart() {
    const { isAllSelectedSku, buyNum, details } = this.data;
    if (!isAllSelectedSku) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: '请选择规格',
        icon: '',
        duration: 1000,
      });
      return;
    }
    const sku = this.getSelectedSku();
    if (!sku) {
      Toast({ context: this, selector: '#t-toast', message: '请选择规格' });
      return;
    }
    try {
      if (!config.useMock) {
        const qty = Number(buyNum);
        await addToCart({
          goodsId: details.spuId,
          specId: sku.skuId,
          num: Number.isFinite(qty) && qty > 0 ? qty : 1,
          selected: 1,
        });
      }
      this.handlePopupHide();
      Toast({
        context: this,
        selector: '#t-toast',
        message: '已加入购物车',
        icon: '',
        duration: 1000,
      });
    } catch (e) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: (e && e.message) || '加购失败',
        icon: '',
      });
    }
  },

  gotoBuy() {
    const { isAllSelectedSku, buyNum } = this.data;
    if (!isAllSelectedSku) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: '请选择规格',
        icon: '',
        duration: 1000,
      });
      return;
    }
    const sku = this.getSelectedSku();
    if (!sku) {
      Toast({ context: this, selector: '#t-toast', message: '请选择规格' });
      return;
    }
    this.handlePopupHide();
    const query = {
      quantity: buyNum,
      storeId: '1',
      spuId: this.data.details.spuId,
      goodsId: this.data.details.spuId,
      goodsName: this.data.details.title,
      skuId: sku.skuId,
      specId: sku.skuId,
      available: this.data.details.available,
      price: sku.price || this.data.details.minSalePrice,
      settlePrice: sku.price || this.data.details.minSalePrice,
      specInfo: this.data.details.specList?.map((item) => ({
        specTitle: item.title,
        specValue: item.specValueList[0].specValue,
      })),
      primaryImage: this.data.details.primaryImage,
      thumb: this.data.details.primaryImage,
      title: this.data.details.title,
      activityType: this.data.activityType || 1,
      activityId: this.data.activityId || null,
    };
    let urlQueryStr = obj2Params({
      goodsRequestList: JSON.stringify([query]),
    });
    urlQueryStr = urlQueryStr ? `?${urlQueryStr}` : '';
    const path = `/pages/order/order-confirm/index${urlQueryStr}`;
    wx.navigateTo({
      url: path,
    });
  },

  specsConfirm() {
    const { buyType } = this.data;
    if (buyType === 1) {
      this.gotoBuy();
    } else {
      this.addCart();
    }
    // this.handlePopupHide();
  },

  changeNum(e) {
    this.setData({
      buyNum: e.detail.buyNum,
    });
  },

  closePromotionPopup() {
    this.setData({
      isShowPromotionPop: false,
    });
  },

  promotionChange(e) {
    const { index } = e.detail;
    wx.navigateTo({
      url: `/pages/promotion/promotion-detail/index?promotion_id=${index}`,
    });
  },

  showPromotionPopup() {
    this.setData({
      isShowPromotionPop: true,
    });
  },

  getDetail(spuId) {
    Promise.all([fetchGood(spuId), fetchActivityList()]).then((res) => {
      const [details, activityList] = res;
      const skuArray = [];
      const { skuList, primaryImage, isPutOnSale, minSalePrice, maxSalePrice, maxLinePrice, soldNum } = details;
      skuList.forEach((item) => {
        skuArray.push({
          skuId: item.skuId,
          quantity: item.stockInfo ? item.stockInfo.stockQuantity : 0,
          stockInfo: item.stockInfo,
          specInfo: item.specInfo,
        });
      });
      const promotionArray = [];
      activityList.forEach((item) => {
        promotionArray.push({
          tag: item.tag || (item.promotionSubCode === 'MYJ' ? '满减' : '活动'),
          label: item.label || '生鲜优惠活动',
        });
      });
      const maxStock = Math.max(
        1,
        ...skuArray.map((s) => Number(s.quantity) || 0),
        Number(details.spuStockQuantity) || 0,
      );
      const rate = details.commentRate || {};
      const totalCount = Number(rate.totalCount != null ? rate.totalCount : 0) || 0;
      const rateGood = rate.goodRate != null ? Number(rate.goodRate) : 100;
      const next = {
        details,
        intro: details.intro || details.descText || '',
        activityList,
        isStock: details.spuStockQuantity > 0,
        maxSalePrice: maxSalePrice ? parseInt(maxSalePrice) : 0,
        maxLinePrice: maxLinePrice ? parseInt(maxLinePrice) : 0,
        minSalePrice: minSalePrice ? parseInt(minSalePrice) : 0,
        list: promotionArray,
        skuArray: skuArray,
        primaryImage,
        soldout: isPutOnSale === 0,
        soldNum,
        limitMaxCount: Math.min(999, maxStock),
      };
      // 先用详情里的评价统计占位，随后 getCommentsStatistics 会再刷新
      if (totalCount >= 0) {
        next.commentsStatistics = {
          ...this.data.commentsStatistics,
          commentCount: totalCount,
          goodCount: Math.round((totalCount * rateGood) / 100),
          goodRate: Math.floor(rateGood * 10) / 10,
        };
      }
      this.setData(next);
    });
  },

  async getCommentsList(spuId) {
    try {
      const id = spuId || this.data.spuId;
      if (!id) return;
      const data = await getGoodsDetailsCommentList(id);
      const { homePageComments, totalCount, goodRate } = data || {};
      const nextState = {
        commentsList: (homePageComments || []).map((item, index) => {
          return {
            goodsSpu: `${item.spuId || id}_${index}`,
            userName: item.userName || '',
            commentScore: item.commentScore,
            commentContent: item.commentContent || '用户未填写评价',
            userHeadUrl: item.isAnonymity ? this.data.anonymityAvatar : item.userHeadUrl || this.data.anonymityAvatar,
            commentImageList: item.commentImageList || [],
          };
        }),
      };
      // 用列表 total 校正数量，避免统计接口滞后时显示 0
      const listTotal = Number(totalCount) || 0;
      const current = Number(this.data.commentsStatistics.commentCount) || 0;
      if (listTotal > current) {
        nextState.commentsStatistics = {
          ...this.data.commentsStatistics,
          commentCount: listTotal,
          goodRate:
            goodRate != null
              ? Math.floor(Number(goodRate) * 10) / 10
              : this.data.commentsStatistics.goodRate,
        };
      }
      this.setData(nextState);
    } catch (error) {
      console.error('comments error:', error);
    }
  },

  onShareAppMessage() {
    // 自定义的返回信息
    const { selectedAttrStr } = this.data;
    let shareSubTitle = '';
    if (selectedAttrStr.indexOf('件') > -1) {
      const count = selectedAttrStr.indexOf('件');
      shareSubTitle = selectedAttrStr.slice(count + 1, selectedAttrStr.length);
    }
    const customInfo = {
      imageUrl: this.data.details.primaryImage,
      title: this.data.details.title + shareSubTitle,
      path: `/pages/goods/details/index?spuId=${this.data.spuId}`,
    };
    return customInfo;
  },

  /** 获取评价统计 */
  async getCommentsStatistics(spuId) {
    try {
      const id = spuId || this.data.spuId;
      if (!id) return;
      const data = await getGoodsDetailsCommentsCount(id);
      const { badCount, commentCount, goodCount, goodRate, hasImageCount, middleCount } = data || {};
      const nextCount = parseInt(`${commentCount || 0}`, 10) || 0;
      const prevCount = Number(this.data.commentsStatistics.commentCount) || 0;
      const listHint = (this.data.commentsList || []).length;
      // 已有评价预览却统计成 0：多半是接口异常，保留更大值
      const finalCount =
        nextCount === 0 && listHint > 0 ? Math.max(prevCount, listHint) : Math.max(nextCount, listHint);
      this.setData({
        commentsStatistics: {
          badCount: parseInt(`${badCount || 0}`, 10) || 0,
          commentCount: finalCount,
          goodCount: parseInt(`${goodCount || 0}`, 10) || 0,
          goodRate: Math.floor(Number(goodRate != null ? goodRate : 100) * 10) / 10,
          hasImageCount: parseInt(`${hasImageCount || 0}`, 10) || 0,
          middleCount: parseInt(`${middleCount || 0}`, 10) || 0,
        },
      });
    } catch (error) {
      console.error('comments statiistics error:', error);
    }
  },

  /** 跳转到评价列表 */
  navToCommentsListPage() {
    wx.navigateTo({
      url: `/pages/goods/comments/index?spuId=${this.data.spuId}`,
    });
  },

  onLoad(query) {
    const { spuId, activityType, activityId } = query;
    this.setData({
      spuId: spuId,
      activityType: activityType ? Number(activityType) : 1,
      activityId: activityId || null,
    });
    this.getDetail(spuId);
    this.getCommentsList(spuId);
    this.getCommentsStatistics(spuId);
  },
});
