import { getMyAllComments } from '../../../../services/good/fetchGoodsDetailsComments';
import dayjs from 'dayjs';

function formatCommentTime(raw) {
  if (raw == null || raw === '') return '';
  if (typeof raw === 'number' || /^\d+$/.test(String(raw))) {
    return dayjs(Number(raw)).format('YYYY/MM/DD HH:mm');
  }
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY/MM/DD HH:mm') : String(raw);
}

Page({
  data: {
    list: [],
    loading: true,
    emptyImg: 'https://tdesign.gtimg.com/miniprogram/template/retail/order/empty-order-list.png',
  },

  onShow() {
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      const rows = await getMyAllComments();
      this.setData({
        list: (rows || []).map((item) => ({
          ...item,
          commentTime: formatCommentTime(item.commentTime),
        })),
        loading: false,
      });
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '加载失败', icon: 'none' });
      this.setData({ list: [], loading: false });
    }
  },

  onGoodsTap(e) {
    const spuId = e.currentTarget.dataset.spuid;
    if (!spuId) return;
    wx.navigateTo({ url: `/pages/goods/details/index?spuId=${spuId}` });
  },
});
