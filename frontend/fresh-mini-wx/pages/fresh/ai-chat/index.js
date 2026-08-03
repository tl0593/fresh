import { sendAiChat, generateCook } from '../../../services/fresh/index';

Page({
  data: {
    sessionKey: '',
    input: '',
    messages: [],
    sending: false,
  },
  onLoad() {
    this.setData({
      sessionKey: `fresh_${Date.now()}`,
      messages: [
        { role: 'ai', content: '你好，我是生鲜助手，可以问我选购、储存或菜谱问题。' },
      ],
    });
  },
  onInput(e) {
    this.setData({ input: e.detail.value });
  },
  async onSend() {
    const text = (this.data.input || '').trim();
    if (!text || this.data.sending) return;
    const messages = this.data.messages.concat([{ role: 'user', content: text }]);
    this.setData({ messages, input: '', sending: true });
    try {
      const res = await sendAiChat({
        sessionKey: this.data.sessionKey,
        userMsg: text,
      });
      this.setData({
        messages: this.data.messages.concat([
          { role: 'ai', content: (res && res.aiReply) || '暂时无法回答，请稍后再试' },
        ]),
        sending: false,
      });
    } catch (e) {
      this.setData({
        messages: this.data.messages.concat([
          { role: 'ai', content: (e && e.message) || 'AI 服务繁忙' },
        ]),
        sending: false,
      });
    }
  },
  async onCook() {
    wx.showLoading({ title: '生成中' });
    try {
      const text = await generateCook('一周家常菜');
      this.setData({
        messages: this.data.messages.concat([
          { role: 'ai', content: typeof text === 'string' ? text : JSON.stringify(text) },
        ]),
      });
    } catch (e) {
      wx.showToast({ title: (e && e.message) || '生成失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },
});
