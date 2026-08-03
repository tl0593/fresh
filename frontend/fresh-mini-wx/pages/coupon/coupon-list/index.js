import { fetchCouponList } from '../../../services/coupon/index';

Page({
  data: {
    pullDownRefreshing: false,
    status: 0,
    list: [
      { text: '可使用', key: 0 },
      { text: '已使用', key: 1 },
      { text: '已失效', key: 2 },
    ],
    couponList: [],
  },

  onLoad() {
    this.init();
  },

  onShow() {
    this.fetchList();
  },

  init() {
    this.fetchList();
  },

  fetchList(status = this.data.status) {
    let statusInFetch = 'default';
    switch (Number(status)) {
      case 0:
        statusInFetch = 'default';
        break;
      case 1:
        statusInFetch = 'useless';
        break;
      case 2:
        statusInFetch = 'disabled';
        break;
      default:
        statusInFetch = 'default';
    }
    return fetchCouponList(statusInFetch).then((couponList) => {
      this.setData({ couponList });
    });
  },

  tabChange(e) {
    const { value } = e.detail;
    this.setData({ status: value });
    this.fetchList(value);
  },

  goCouponCenterHandle() {
    wx.navigateTo({ url: '/pages/coupon/coupon-center/index' });
  },

  onPullDownRefresh_() {
    this.setData({ couponList: [], pullDownRefreshing: true }, () => {
      this.fetchList()
        .then(() => this.setData({ pullDownRefreshing: false }))
        .catch(() => this.setData({ pullDownRefreshing: false }));
    });
  },
});
