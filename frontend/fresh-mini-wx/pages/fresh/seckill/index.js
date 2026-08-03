import { fetchSeckillList } from '../../../services/fresh/index';

Page({
  data: {
    list: [],
    loading: true,
  },
  onShow() {
    this.load();
  },
  async load() {
    this.setData({ loading: true });
    try {
      const list = await fetchSeckillList();
      this.setData({ list, loading: false });
    } catch (e) {
      this.setData({ list: [], loading: false });
      wx.showToast({ title: (e && e.message) || '加载失败', icon: 'none' });
    }
  },
  goDetail(e) {
    const { goodsid, id } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/goods/details/index?spuId=${goodsid}&activityType=3&activityId=${id}`,
    });
  },
});
