import Dialog from 'tdesign-miniprogram/dialog/index';
import Toast from 'tdesign-miniprogram/toast/index';
import { priceFormat } from '../../../utils/util';
import { OrderStatus, ServiceType, ServiceReceiptStatus } from '../config';
import reasonSheet from '../components/reason-sheet/reasonSheet';
import {
  fetchRightsPreview,
  dispatchConfirmReceived,
  fetchApplyReasonList,
  dispatchApplyService,
} from '../../../services/order/applyService';

Page({
  query: {},
  data: {
    uploading: false, // 凭证上传状态
    canApplyReturn: true, // 是否可退货
    goodsInfo: {},
    /** 订单全部商品：名称 + 数量 */
    goodsList: [],
    receiptStatusList: [
      { desc: '未收到货', status: ServiceReceiptStatus.NOT_RECEIPTED },
      { desc: '已收到货', status: ServiceReceiptStatus.RECEIPTED },
    ],
    applyReasons: [],
    serviceType: null, // 20-仅退款，10-退货退款
    serviceFrom: {
      returnNum: 1,
      receiptStatus: { desc: '请选择', status: null },
      applyReason: { desc: '请选择', type: null },
      // max-填写上限(单位分)，current-当前值(单位分)，temp输入框中的值(单位元)
      amount: { max: 0, current: 0, temp: 0, focus: false },
      remark: '',
      rightsImageUrls: [],
    },
    maxApplyNum: 2, // 最大可申请售后的商品数
    amountTip: '',
    showReceiptStatusDialog: false,
    validateRes: {
      valid: false,
      msg: '',
    },
    submitting: false,
    inputDialogVisible: false,
    uploadGridConfig: {
      column: 3,
      width: 212,
      height: 212,
    },
    serviceRequireType: '',
  },

  setWatcher(key, callback) {
    let lastData = this.data;
    const keys = key.split('.');
    keys.slice(0, -1).forEach((k) => {
      lastData = lastData[k];
    });
    const lastKey = keys[keys.length - 1];
    this.observe(lastData, lastKey, callback);
  },

  observe(data, k, callback) {
    let val = data[k];
    Object.defineProperty(data, k, {
      configurable: true,
      enumerable: true,
      set: (value) => {
        val = value;
        callback();
      },
      get: () => {
        return val;
      },
    });
  },

  validate() {
    let valid = true;
    let msg = '';
    // 检查必填项
    if (!this.data.serviceFrom.applyReason.type) {
      valid = false;
      msg = '请填写退款原因';
    } else if (!this.data.serviceFrom.amount.current) {
      valid = false;
      msg = '请填写退款金额';
    }
    if (this.data.serviceFrom.amount.current <= 0) {
      valid = false;
      msg = '退款金额必须大于0';
    }
    this.setData({ validateRes: { valid, msg } });
  },

  onLoad(query) {
    this.query = query;
    if (!this.checkQuery()) return;
    this.setData({
      canApplyReturn: query.canApplyReturn === 'true',
    });
    this.init();
    this.inputDialog = this.selectComponent('#input-dialog');
    this.setWatcher('serviceFrom.returnNum', this.validate.bind(this));
    this.setWatcher('serviceFrom.applyReason', this.validate.bind(this));
    this.setWatcher('serviceFrom.amount', this.validate.bind(this));
    this.setWatcher('serviceFrom.rightsImageUrls', this.validate.bind(this));
  },

  async init() {
    try {
      await this.refresh();
    } catch (e) {}
  },

  checkQuery() {
    const { orderNo, skuId } = this.query;
    if (!orderNo) {
      Dialog.alert({
        content: '请先选择订单',
      }).then(() => {
        wx.redirectTo({ url: 'pages/order/order-list/index' });
      });
      return false;
    }
    if (!skuId) {
      Dialog.alert({
        content: '请先选择商品',
      }).then(() => {
        wx.redirectTo(`pages/order/order-detail/index?orderNo=${orderNo}`);
      });
      return false;
    }
    return true;
  },

  async refresh() {
    wx.showLoading({ title: 'loading' });
    try {
      const res = await this.getRightsPreview();
      wx.hideLoading();
      const goodsInfo = {
        id: res.data.skuId,
        thumb: res.data.goodsInfo && res.data.goodsInfo.skuImage,
        title: res.data.goodsInfo && res.data.goodsInfo.goodsName,
        spuId: res.data.spuId,
        skuId: res.data.skuId,
        specs: ((res.data.goodsInfo && res.data.goodsInfo.specInfo) || []).map((s) => s.specValue),
        paidAmountEach: res.data.paidAmountEach,
        boughtQuantity: res.data.boughtQuantity,
      };
      const goodsList = (res.data.goodsList || []).map((g) => ({
        orderItemId: g.orderItemId,
        goodsName: g.goodsName || '生鲜商品',
        num: Number(g.num || 1),
      }));
      this.setData({
        goodsInfo,
        goodsList,
        'serviceFrom.amount': {
          max: res.data.refundableAmount,
          current: res.data.refundableAmount,
        },
        'serviceFrom.returnNum': res.data.numOfSku,
        amountTip: `最多可申请退款¥ ${priceFormat(res.data.refundableAmount, 2)}（社区自提，无运费）`,
        maxApplyNum: res.data.numOfSkuAvailable,
      });
    } catch (err) {
      wx.hideLoading();
      throw err;
    }
  },

  async getRightsPreview() {
    const { orderNo, skuId, spuId, orderItemId, payAmt, num } = this.query;
    const params = {
      orderNo,
      skuId,
      spuId,
      orderItemId,
      // 入口已带订单实付（分），预览接口失败时可作为兜底
      payAmt,
      num,
      numOfSku: this.data.serviceFrom.returnNum,
    };
    const res = await fetchRightsPreview(params);
    // 缓存订单项，提交售后时带上
    if (res && res.data && res.data.orderItemId) {
      this.query.orderItemId = res.data.orderItemId;
    }
    return res;
  },

  onApplyOnlyRefund() {
    wx.setNavigationBarTitle({ title: '申请退款' });
    this.setData({ serviceRequireType: 'REFUND_MONEY' });
    this.switchReceiptStatus(0);
  },

  onApplyReturnGoods() {
    wx.setNavigationBarTitle({ title: '申请退货退款' });
    this.setData({ serviceRequireType: 'REFUND_GOODS' });
    const orderStatus = parseInt(this.query.orderStatus);
    Promise.resolve()
      .then(() => {
        if (orderStatus === OrderStatus.PENDING_RECEIPT) {
          return Dialog.confirm({
            title: '订单商品是否已经收到货',
            content: '',
            confirmBtn: '确认收货，并申请退货',
            cancelBtn: '未收到货',
          }).then(() => {
            return dispatchConfirmReceived({
              parameter: {
                logisticsNo: this.query.logisticsNo,
                orderNo: this.query.orderNo,
              },
            });
          });
        }
        return;
      })
      .then(() => {
        this.setData({ serviceType: ServiceType.RETURN_GOODS });
        this.switchReceiptStatus(1);
      });
  },

  onApplyReturnGoodsStatus() {
    reasonSheet({
      show: true,
      title: '选择退款原因',
      options: this.data.applyReasons.map((r) => ({
        title: r.desc,
      })),
      showConfirmButton: true,
      showCancelButton: true,
      emptyTip: '请选择退款原因',
    }).then((indexes) => {
      this.setData({
        'serviceFrom.applyReason': this.data.applyReasons[indexes[0]],
      });
    });
  },

  onChangeReturnNum(e) {
    const { value } = e.detail;
    this.setData({
      'serviceFrom.returnNum': value,
    });
  },

  onApplyGoodsStatus() {
    reasonSheet({
      show: true,
      title: '请选择收货状态',
      options: this.data.receiptStatusList.map((r) => ({
        title: r.desc,
      })),
      showConfirmButton: true,
      emptyTip: '请选择收货状态',
    }).then((indexes) => {
      this.setData({
        'serviceFrom.receiptStatus': this.data.receiptStatusList[indexes[0]],
      });
    });
  },

  switchReceiptStatus(index) {
    const statusItem = this.data.receiptStatusList[index];
    // 没有找到对应的状态，则清空/初始化
    if (!statusItem) {
      this.setData({
        showReceiptStatusDialog: false,
        'serviceFrom.receiptStatus': { desc: '请选择', status: null },
        'serviceFrom.applyReason': { desc: '请选择', type: null }, // 收货状态改变时，初始化申请原因
        applyReasons: [],
      });
      return;
    }
    // 仅选中项与当前项不一致时，才切换申请原因列表applyReasons
    if (!statusItem || statusItem.status === this.data.serviceFrom.receiptStatus.status) {
      this.setData({ showReceiptStatusDialog: false });
      return;
    }
    this.getApplyReasons(statusItem.status).then((reasons) => {
      this.setData({
        showReceiptStatusDialog: false,
        'serviceFrom.receiptStatus': statusItem,
        'serviceFrom.applyReason': { desc: '请选择', type: null }, // 收货状态改变时，重置申请原因
        applyReasons: reasons,
      });
    });
  },

  getApplyReasons(receiptStatus) {
    const params = { rightsReasonType: receiptStatus };
    return fetchApplyReasonList(params)
      .then((res) => {
        return res.data.rightsReasonList.map((reason) => ({
          type: reason.id,
          desc: reason.desc,
        }));
      })
      .catch(() => {
        return [];
      });
  },

  onReceiptStatusDialogConfirm(e) {
    const { index } = e.currentTarget.dataset;
    this.switchReceiptStatus(index);
  },

  onAmountTap() {
    this.setData({
      'serviceFrom.amount.temp': priceFormat(this.data.serviceFrom.amount.current),
      'serviceFrom.amount.focus': true,
      inputDialogVisible: true,
    });
    this.inputDialog.setData({
      cancelBtn: '取消',
      confirmBtn: '确定',
    });
    this.inputDialog._onConfirm = () => {
      this.setData({
        'serviceFrom.amount.current': this.data.serviceFrom.amount.temp * 100,
      });
    };
    this.inputDialog._onCancel = () => {};
  },

  // 对输入的值进行过滤
  onAmountInput(e) {
    let { value } = e.detail;
    const regRes = value.match(/\d+(\.?\d*)?/); // 输入中，允许末尾为小数点
    value = regRes ? regRes[0] : '';
    this.setData({ 'serviceFrom.amount.temp': value });
  },

  // 失去焦点时，更严格的过滤并转化为float
  onAmountBlur(e) {
    let { value } = e.detail;
    const regRes = value.match(/\d+(\.?\d+)?/); // 失去焦点时，不允许末尾为小数点
    value = regRes ? regRes[0] : '0';
    value = parseFloat(value) * 100;
    if (value > this.data.serviceFrom.amount.max) {
      value = this.data.serviceFrom.amount.max;
    }
    this.setData({
      'serviceFrom.amount.temp': priceFormat(value),
      'serviceFrom.amount.focus': false,
    });
  },

  onAmountFocus() {
    this.setData({ 'serviceFrom.amount.focus': true });
  },

  onRemarkChange(e) {
    const { value } = e.detail;
    this.setData({
      'serviceFrom.remark': value,
    });
  },

  // 发起申请售后请求
  onSubmit() {
    this.submitCheck().then(() => {
      const params = {
        rights: {
          orderNo: this.query.orderNo,
          orderItemId: this.query.orderItemId,
          payAmt: this.query.payAmt,
          refundRequestAmount: this.data.serviceFrom.amount.current,
          rightsImageUrls: this.data.serviceFrom.rightsImageUrls,
          rightsReasonDesc: this.data.serviceFrom.applyReason.desc,
          rightsReasonType: this.data.serviceFrom.receiptStatus.status,
          rightsType: this.data.serviceType,
        },
        rightsItem: [
          {
            orderItemId: this.query.orderItemId,
            itemTotalAmount: this.data.serviceFrom.amount.current,
            rightsQuantity: this.data.serviceFrom.returnNum,
            skuId: this.query.skuId,
            spuId: this.query.spuId,
          },
        ],
        refundMemo: this.data.serviceFrom.remark,
      };
      this.setData({ submitting: true });
      // 发起申请售后请求
      dispatchApplyService(params)
        .then((res) => {
          Toast({
            context: this,
            selector: '#t-toast',
            message: '申请成功',
            icon: '',
          });

          wx.redirectTo({
            url: `/pages/order/after-service-detail/index?rightsNo=${res.data.rightsNo}`,
          });
        })
        .then(() => this.setData({ submitting: false }))
        .catch(() => this.setData({ submitting: false }));
    });
  },

  submitCheck() {
    return new Promise((resolve) => {
      const { msg, valid } = this.data.validateRes;
      if (!valid) {
        Toast({
          context: this,
          selector: '#t-toast',
          message: msg,
          icon: '',
        });
        return;
      }
      resolve();
    });
  },

  /** 去重，避免 add/success 各触发一次导致同一张图出现两次 */
  normalizeUploadFiles(files) {
    const list = files || [];
    const seen = new Set();
    const result = [];
    list.forEach((f) => {
      const key = (f && (f.tempFilePath || f.path || f.url)) || (typeof f === 'string' ? f : '');
      if (!key || seen.has(key)) return;
      seen.add(key);
      result.push(f);
    });
    return result;
  },

  handleAdd(e) {
    const files = this.normalizeUploadFiles((e.detail || {}).files);
    this.setData({ 'serviceFrom.rightsImageUrls': files });
  },

  // 无 action 时 success 也可能触发；只同步列表并去重，避免叠图
  handleSuccess(e) {
    const files = this.normalizeUploadFiles((e.detail || {}).files);
    this.setData({ 'serviceFrom.rightsImageUrls': files });
  },

  handleRemove(e) {
    const { index } = e.detail || {};
    const rightsImageUrls = (this.data.serviceFrom.rightsImageUrls || []).slice();
    if (typeof index === 'number' && index >= 0) {
      rightsImageUrls.splice(index, 1);
      this.setData({ 'serviceFrom.rightsImageUrls': this.normalizeUploadFiles(rightsImageUrls) });
    }
  },

  handleComplete() {
    this.setData({
      uploading: false,
    });
  },

  handleSelectChange() {
    this.setData({
      uploading: true,
    });
  },
});
