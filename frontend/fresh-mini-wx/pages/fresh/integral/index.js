import {
  fetchIntegralCouponList,
  exchangeIntegralCoupon,
  fetchLotteryPrizes,
  drawLottery,
} from '../../../services/coupon/index';
import { fetchIntegralLogs } from '../../../services/usercenter/fetchUsercenter';
import request from '../../../utils/request';
import { ensureLogin } from '../../../services/auth/login';

Page({
  data: {
    tab: 'exchange', // exchange | lottery | log
    integral: 0,
    exchangeList: [],
    prizes: [],
    costIntegral: 0,
    logs: [],
    loading: true,
    drawing: false,
    exchangingId: null,
    drawResult: '',
  },

  onShow() {
    this.refreshAll();
  },

  async refreshAll() {
    this.setData({ loading: true });
    try {
      await ensureLogin();
      const me = await request.get('/api/user/integral/balance');
      const integral = (me && me.integral) || 0;
      this.setData({ integral: Number(integral) || 0 });
      try {
        const cached = wx.getStorageSync('userInfo') || {};
        cached.integral = Number(integral) || 0;
        wx.setStorageSync('userInfo', cached);
      } catch (e) {
        /* ignore */
      }
    } catch (e) {
      /* ignore */
    }
    await Promise.all([this.loadExchange(), this.loadPrizes(), this.loadLogs()]);
    this.setData({ loading: false });
  },

  async loadExchange() {
    try {
      const exchangeList = await fetchIntegralCouponList();
      this.setData({ exchangeList: exchangeList || [] });
    } catch (e) {
      this.setData({ exchangeList: [] });
    }
  },

  async loadPrizes() {
    try {
      const prizes = await fetchLotteryPrizes();
      const costIntegral =
        (prizes || []).reduce((min, p) => {
          const c = Number(p.costIntegral || 0);
          if (!c) return min;
          return min === 0 ? c : Math.min(min, c);
        }, 0) || 0;
      this.setData({ prizes: prizes || [], costIntegral });
    } catch (e) {
      this.setData({ prizes: [], costIntegral: 0 });
    }
  },

  async loadLogs() {
    try {
      const logs = await fetchIntegralLogs();
      this.setData({ logs: logs || [] });
    } catch (e) {
      this.setData({ logs: [] });
    }
  },

  onTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab || tab === this.data.tab) return;
    this.setData({ tab, drawResult: '' });
  },

  async onExchange(e) {
    const id = e.currentTarget.dataset.id;
    const cost = Number(e.currentTarget.dataset.cost || 0);
    if (!id || this.data.exchangingId) return;
    if (this.data.integral < cost) {
      wx.showToast({ title: '积分不足', icon: 'none' });
      return;
    }
    const ok = await new Promise((resolve) => {
      wx.showModal({
        title: '确认兑换',
        content: `将消耗 ${cost} 积分兑换该优惠券`,
        success: (res) => resolve(!!res.confirm),
      });
    });
    if (!ok) return;
    this.setData({ exchangingId: id });
    try {
      await exchangeIntegralCoupon(id);
      wx.showToast({ title: '兑换成功', icon: 'success' });
      await this.refreshAll();
    } catch (err) {
      wx.showToast({ title: (err && err.message) || '兑换失败', icon: 'none' });
    } finally {
      this.setData({ exchangingId: null });
    }
  },

  async onDraw() {
    if (this.data.drawing) return;
    if (!this.data.prizes.length) {
      wx.showToast({ title: '暂无抽奖活动', icon: 'none' });
      return;
    }
    if (this.data.integral < this.data.costIntegral) {
      wx.showToast({ title: '积分不足', icon: 'none' });
      return;
    }
    this.setData({ drawing: true, drawResult: '抽奖中...' });
    try {
      const res = await drawLottery();
      const message = (res && res.message) || '抽奖完成';
      this.setData({ drawResult: message });
      wx.showToast({ title: message, icon: 'none', duration: 2500 });
      await this.refreshAll();
    } catch (err) {
      this.setData({ drawResult: '' });
      wx.showToast({ title: (err && err.message) || '抽奖失败', icon: 'none' });
    } finally {
      this.setData({ drawing: false });
    }
  },
});
