import Toast from 'tdesign-miniprogram/toast/index';
import Dialog from 'tdesign-miniprogram/dialog/index';
import { OrderButtonTypes } from '../../config';
import { isItemCommented } from '../../../../services/order/orderSubmitComment';

Component({
  options: {
    addGlobalClass: true,
  },
  properties: {
    order: {
      type: Object,
      observer(order) {
        // 判定有传goodsIndex ，则认为是商品button bar, 仅显示申请售后按钮
        if (this.properties?.goodsIndex !== null) {
          const goods = order.goodsList[Number(this.properties.goodsIndex)];
          this.setData({
            buttons: {
              left: [],
              right: (goods.buttons || []).filter((b) => b.type == OrderButtonTypes.APPLY_REFUND),
            },
          });
          return;
        }
        // 订单的button bar 不显示申请售后按钮
        const buttonsRight = (order.buttons || [])
          // .filter((b) => b.type !== OrderButtonTypes.APPLY_REFUND)
          .map((button) => {
            //邀请好友拼团按钮
            if (button.type === OrderButtonTypes.INVITE_GROUPON && order.groupInfoVo) {
              const {
                groupInfoVo: { groupId, promotionId, remainMember, groupPrice },
                goodsList,
              } = order;
              const goodsImg = goodsList[0] && goodsList[0].imgUrl;
              const goodsName = goodsList[0] && goodsList[0].name;
              return {
                ...button,
                openType: 'share',
                dataShare: {
                  goodsImg,
                  goodsName,
                  groupId,
                  promotionId,
                  remainMember,
                  groupPrice,
                  storeId: order.storeId,
                },
              };
            }
            return button;
          });
        // 删除订单按钮单独挪到左侧
        const deleteBtnIndex = buttonsRight.findIndex((b) => b.type === OrderButtonTypes.DELETE);
        let buttonsLeft = [];
        if (deleteBtnIndex > -1) {
          buttonsLeft = buttonsRight.splice(deleteBtnIndex, 1);
        }
        this.setData({
          buttons: {
            left: buttonsLeft,
            right: buttonsRight,
          },
        });
      },
    },
    goodsIndex: {
      type: Number,
      value: null,
    },
    isBtnMax: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    order: {},
    buttons: {
      left: [],
      right: [],
    },
  },

  methods: {
    // 点击【订单操作】按钮，根据按钮类型分发
    onOrderBtnTap(e) {
      const { type } = e.currentTarget.dataset;
      switch (type) {
        case OrderButtonTypes.DELETE:
          this.onDelete(this.data.order);
          break;
        case OrderButtonTypes.CANCEL:
          this.onCancel(this.data.order);
          break;
        case OrderButtonTypes.CONFIRM:
          this.onConfirm(this.data.order);
          break;
        case OrderButtonTypes.PAY:
          this.onPay(this.data.order);
          break;
        case OrderButtonTypes.APPLY_REFUND:
          this.onApplyRefund(this.data.order);
          break;
        case OrderButtonTypes.VIEW_REFUND:
          this.onViewRefund(this.data.order);
          break;
        case OrderButtonTypes.COMMENT:
          this.onAddComment(this.data.order);
          break;
        case OrderButtonTypes.INVITE_GROUPON:
          //分享邀请好友拼团
          break;
        case OrderButtonTypes.REBUY:
          this.onBuyAgain(this.data.order);
      }
    },

    onCancel() {
      Toast({
        context: this,
        selector: '#t-toast',
        message: '暂不支持用户取消，超时未支付将自动取消',
        icon: 'info-circle',
      });
    },

    onConfirm() {
      Dialog.confirm({
        title: '确认已到自提点取货？',
        content: '请确认您已完成社区自提',
        confirmBtn: '确认已自提',
        cancelBtn: '取消',
      })
        .then(() => {
          Toast({
            context: this,
            selector: '#t-toast',
            message: '自提确认功能待后端开放，请联系管理员核销',
            icon: 'check-circle',
          });
        })
        .catch(() => {});
    },

    onPay(order) {
      const { config } = require('../../../../config/index');
      if (!config.useFreshPay) {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '你点击了去支付',
          icon: 'check-circle',
        });
        return;
      }
      const { wechatPayOrder } = require('../../order-confirm/pay');
      wechatPayOrder({
        tradeNo: order.orderNo,
        orderId: order.orderNo,
        payAmt: order.amount || order.paymentAmount,
        dialogOnCancel: false,
      });
    },

    onBuyAgain() {
      Toast({
        context: this,
        selector: '#t-toast',
        message: '你点击了再次购买',
        icon: 'check-circle',
      });
    },

    onApplyRefund(order) {
      const idx = this.properties.goodsIndex;
      const goods =
        idx !== null && idx !== undefined
          ? order.goodsList[Number(idx)]
          : order.goodsList && order.goodsList[0];
      const params = {
        orderNo: order.orderNo,
        orderItemId: goods?.id || '',
        skuId: goods?.skuId ?? '',
        spuId: goods?.spuId ?? '',
        orderStatus: order.status,
        logisticsNo: order.logisticsNo || '',
        price: goods?.price ?? order.amount ?? 0,
        num: goods?.num ?? 1,
        createTime: order.createTime || '',
        orderAmt: order.totalAmount,
        payAmt: order.amount,
        canApplyReturn: true,
      };
      const paramsStr = Object.keys(params)
        .map((k) => `${k}=${encodeURIComponent(params[k])}`)
        .join('&');
      wx.navigateTo({ url: `/pages/order/apply-service/index?${paramsStr}` });
    },

    onViewRefund() {
      wx.navigateTo({ url: '/pages/order/after-service-list/index' });
    },

    /** 按商品评价：优先进入首个未评价订单项 */
    onAddComment(order) {
      const list = order?.goodsList || [];
      const pending = list.find((g) => !isItemCommented(g)) || list[0] || {};
      const orderItemId = pending.id || '';
      const orderNo = order?.orderNo || '';
      if (!orderItemId && !orderNo) {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '无法评价：缺少订单信息',
          icon: '',
        });
        return;
      }
      wx.navigateTo({
        url: `/pages/goods/comments/create/index?orderNo=${encodeURIComponent(orderNo)}&orderItemId=${encodeURIComponent(orderItemId || '')}`,
      });
    },
  },
});
