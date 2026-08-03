import { fetchUserCenter } from '../../services/usercenter/fetchUsercenter';
import Toast from 'tdesign-miniprogram/toast/index';

const menuData = [
  [
    {
      title: '自提地址',
      tit: '',
      url: '',
      type: 'address',
    },
    {
      title: '优惠券',
      tit: '',
      url: '',
      type: 'coupon',
    },
    {
      title: '积分',
      tit: '',
      url: '',
      type: 'point',
    },
  ],
  [
    {
      title: '今日团购',
      tit: '',
      url: '',
      type: 'group',
    },
    {
      title: '限时秒杀',
      tit: '',
      url: '',
      type: 'seckill',
    },
    {
      title: 'AI 客服',
      tit: '',
      url: '',
      type: 'ai',
    },
    {
      title: '站内消息',
      tit: '',
      url: '',
      type: 'message',
    },
    {
      title: '我的评价',
      tit: '',
      url: '',
      type: 'myComment',
    },
  ],
  [
    {
      title: '客服热线',
      tit: '',
      url: '',
      type: 'service',
      icon: 'service',
    },
  ],
];

const orderTagInfos = [
  {
    title: '待付款',
    iconName: 'wallet',
    orderNum: 0,
    tabType: 0,
    status: 1,
  },
  {
    title: '待自提',
    iconName: 'package',
    orderNum: 0,
    tabType: 1,
    status: 1,
  },
  {
    title: '已完成',
    iconName: 'comment',
    orderNum: 0,
    tabType: 2,
    status: 1,
  },
  {
    title: '已取消',
    iconName: 'deliver',
    orderNum: 0,
    tabType: 3,
    status: 1,
  },
  {
    title: '退款/售后',
    iconName: 'exchang',
    orderNum: 0,
    tabType: 4,
    status: 1,
  },
];

const getDefaultData = () => ({
  showMakePhone: false,
  userInfo: {
    avatarUrl: '',
    nickName: '正在登录...',
    phoneNumber: '',
  },
  menuData,
  orderTagInfos,
  customerServiceInfo: {},
  currAuthStep: 1,
  showKefu: true,
  versionNo: '',
});

Page({
  data: getDefaultData(),

  onLoad() {
    this.getVersionInfo();
  },

  onShow() {
    this.getTabBar().init();
    this.init();
  },
  onPullDownRefresh() {
    this.init();
  },

  init() {
    this.fetUseriInfoHandle();
  },

  fetUseriInfoHandle() {
    fetchUserCenter().then(({ userInfo, countsData, orderTagInfos: orderInfo, customerServiceInfo }) => {
      menuData?.[0].forEach((v) => {
        countsData.forEach((counts) => {
          if (counts.type === v.type) {
            // eslint-disable-next-line no-param-reassign
            v.tit = counts.num;
          }
        });
      });
      const info = orderTagInfos.map((v, index) => ({
        ...v,
        ...(orderInfo[index] || {}),
      }));
      this.setData({
        userInfo,
        menuData,
        orderTagInfos: info,
        customerServiceInfo,
        currAuthStep: 2,
      });
      wx.stopPullDownRefresh();
    });
  },

  onClickCell({ currentTarget }) {
    const { type } = currentTarget.dataset;

    switch (type) {
      case 'address': {
        wx.navigateTo({ url: '/pages/user/address/list/index' });
        break;
      }
      case 'service': {
        this.openMakePhone();
        break;
      }
      case 'point': {
        wx.navigateTo({ url: '/pages/fresh/integral/index' });
        break;
      }
      case 'coupon': {
        wx.navigateTo({ url: '/pages/coupon/coupon-list/index' });
        break;
      }
      case 'group': {
        wx.navigateTo({ url: '/pages/fresh/group/index' });
        break;
      }
      case 'seckill': {
        wx.navigateTo({ url: '/pages/fresh/seckill/index' });
        break;
      }
      case 'ai': {
        wx.navigateTo({ url: '/pages/fresh/ai-chat/index' });
        break;
      }
      case 'message': {
        wx.navigateTo({ url: '/pages/fresh/message/index' });
        break;
      }
      case 'myComment': {
        wx.navigateTo({ url: '/pages/goods/comments/mine/index' });
        break;
      }
      default: {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '未知跳转',
          icon: '',
          duration: 1000,
        });
        break;
      }
    }
  },

  jumpNav(e) {
    const status = e.detail.tabType;
    // 「退款/售后」进入售后列表，其余状态进订单列表对应 Tab
    if (Number(status) === 4) {
      wx.navigateTo({ url: '/pages/order/after-service-list/index' });
      return;
    }
    wx.navigateTo({ url: `/pages/order/order-list/index?status=${status}` });
  },

  jumpAllOrder() {
    wx.navigateTo({ url: '/pages/order/order-list/index' });
  },

  openMakePhone() {
    this.setData({ showMakePhone: true });
  },

  closeMakePhone() {
    this.setData({ showMakePhone: false });
  },

  call() {
    wx.makePhoneCall({
      phoneNumber: this.data.customerServiceInfo.servicePhone,
    });
  },

  gotoUserEditPage() {
    const { currAuthStep } = this.data;
    if (currAuthStep === 2) {
      wx.navigateTo({ url: '/pages/user/person-info/index' });
    } else {
      this.fetUseriInfoHandle();
    }
  },

  getVersionInfo() {
    const versionInfo = wx.getAccountInfoSync();
    const { version, envVersion = __wxConfig } = versionInfo.miniProgram;
    this.setData({
      versionNo: envVersion === 'release' ? version : envVersion,
    });
  },
});
