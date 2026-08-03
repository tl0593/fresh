const statusMap = {
  default: { text: '去使用', theme: 'primary' },
  useless: { text: '已使用', theme: 'default' },
  disabled: { text: '已过期', theme: 'default' },
  receive: { text: '领取', theme: 'primary' },
};
Component({
  options: {
    addGlobalClass: true,
    multipleSlots: true,
  },

  externalClasses: ['coupon-class'],

  properties: {
    couponDTO: {
      type: Object,
      value: {},
    },
  },

  data: {
    btnText: '',
    btnTheme: '',
  },

  observers: {
    couponDTO: function (couponDTO) {
      if (!couponDTO) {
        return;
      }
      const key = couponDTO.action === 'receive' ? 'receive' : couponDTO.status;
      const statusInfo = statusMap[key] || statusMap.default;
      this.setData({
        btnText: statusInfo.text,
        btnTheme: statusInfo.theme,
      });
    },
  },

  methods: {
    gotoDetail() {
      const dto = this.data.couponDTO || {};
      if (dto.action === 'receive') {
        return;
      }
      wx.navigateTo({
        url: `/pages/coupon/coupon-detail/index?id=${dto.key}`,
      });
    },

    onActionTap() {
      const dto = this.data.couponDTO || {};
      if (dto.action === 'receive') {
        this.triggerEvent('receive', { id: dto.id || dto.key });
        return;
      }
      if (dto.status === 'default') {
        // 去使用：跳转首页选购
        wx.switchTab({ url: '/pages/home/home' });
        return;
      }
    },
  },
});
