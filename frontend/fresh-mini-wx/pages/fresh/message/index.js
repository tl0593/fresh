import { fetchInnerMessages, markMessageRead } from '../../../services/fresh/index';

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
      const list = await fetchInnerMessages();
      this.setData({ list, loading: false });
    } catch (e) {
      this.setData({ list: [], loading: false });
      wx.showToast({ title: (e && e.message) || '加载失败', icon: 'none' });
    }
  },
  async onRead(e) {
    const { id, index } = e.currentTarget.dataset;
    try {
      await markMessageRead(id);
      this.setData({ [`list[${index}].readFlag`]: 1 });
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '标记失败', icon: 'none' });
    }
  },
});
