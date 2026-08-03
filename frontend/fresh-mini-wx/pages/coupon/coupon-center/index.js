import {
  fetchCouponCenterList,
  receiveCoupon,
  receiveCouponBatch,
  fetchSeckillCouponList,
  grabSeckillCoupon,
} from '../../../services/coupon/index';

Page({
  data: {
    pullDownRefreshing: false,
    couponList: [],
    seckillList: [],
    batching: false,
    grabbingId: null,
    nowHour: 0,
  },

  onLoad() {
    this.fetchAll();
  },

  onShow() {
    this.fetchAll();
  },

  fetchAll() {
    const nowHour = new Date().getHours();
    this.setData({ nowHour });
    return Promise.all([this.fetchList(), this.fetchSeckill()]);
  },

  fetchList() {
    return fetchCouponCenterList()
      .then((couponList) => {
        this.setData({ couponList });
      })
      .catch((e) => {
        wx.showToast({ title: (e && e.message) || '加载失败', icon: 'none' });
      });
  },

  fetchSeckill() {
    return fetchSeckillCouponList()
      .then((seckillList) => {
        this.setData({ seckillList: seckillList || [] });
      })
      .catch(() => {
        this.setData({ seckillList: [] });
      });
  },

  onReceive(e) {
    const id = e.detail && (e.detail.id || e.detail.key);
    if (!id) return;
    receiveCoupon(id)
      .then(() => {
        wx.showToast({ title: '领取成功', icon: 'success' });
        this.fetchList();
      })
      .catch((err) => {
        wx.showToast({ title: (err && err.message) || '领取失败', icon: 'none' });
      });
  },

  async onBatchReceive() {
    if (this.data.batching) return;
    if (!this.data.couponList.length) {
      wx.showToast({ title: '暂无可领优惠券', icon: 'none' });
      return;
    }
    this.setData({ batching: true });
    try {
      const res = await receiveCouponBatch();
      wx.showToast({
        title: (res && res.message) || `成功领取 ${(res && res.successCount) || 0} 张`,
        icon: 'none',
        duration: 2200,
      });
      this.fetchList();
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '领取失败', icon: 'none' });
    } finally {
      this.setData({ batching: false });
    }
  },

  async onGrab(e) {
    const id = e.currentTarget.dataset.id;
    const can = e.currentTarget.dataset.can;
    if (!id || !can || this.data.grabbingId) return;
    this.setData({ grabbingId: id });
    try {
      await grabSeckillCoupon(id);
      wx.showToast({ title: '抢券成功', icon: 'success' });
      this.fetchSeckill();
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '抢券失败', icon: 'none' });
    } finally {
      this.setData({ grabbingId: null });
    }
  },

  goIntegralMall() {
    wx.navigateTo({ url: '/pages/fresh/integral/index' });
  },

  onPullDownRefresh_() {
    this.setData({ pullDownRefreshing: true }, () => {
      this.fetchAll()
        .then(() => this.setData({ pullDownRefreshing: false }))
        .catch(() => this.setData({ pullDownRefreshing: false }));
    });
  },
});
