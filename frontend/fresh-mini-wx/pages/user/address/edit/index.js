import Toast from 'tdesign-miniprogram/toast/index';
import { fetchDeliveryAddress, saveDeliveryAddress } from '../../../../services/address/fetchAddress';
import { resolveAddress, rejectAddress } from '../../../../services/address/list';
import { config } from '../../../../config/index';

const innerPhoneReg = '^1(?:3\\d|4[4-9]|5[0-35-9]|6[67]|7[0-8]|8\\d|9\\d)\\d{8}$';
const innerNameReg = '^[a-zA-Z\\d\\u4e00-\\u9fa5]+$';

Page({
  options: {
    multipleSlots: true,
  },
  externalClasses: ['theme-wrapper-class'],
  data: {
    locationState: {
      addressId: '',
      community: '',
      detailAddress: '',
      detailAddr: '',
      isDefault: false,
      name: '',
      phone: '',
      isEdit: false,
    },
    submitActive: false,
  },
  privateData: {
    verifyTips: '',
  },
  onLoad(options) {
    const { id } = options;
    this.init(id);
  },

  onUnload() {
    if (!this.hasSava) {
      rejectAddress();
    }
  },

  hasSava: false,

  init(id) {
    if (id) {
      this.getAddressDetail(Number(id));
    }
  },
  getAddressDetail(id) {
    fetchDeliveryAddress(id).then((detail) => {
      this.setData(
        {
          locationState: {
            ...detail,
            addressId: detail.id || detail.addressId,
            community: detail.community || detail.districtName || '',
            detailAddress: detail.detailAddr || detail.detailAddress || '',
            isDefault: detail.isDefault === 1,
          },
        },
        () => {
          const { isLegal, tips } = this.onVerifyInputLegal();
          this.setData({ submitActive: isLegal });
          this.privateData.verifyTips = tips;
        },
      );
    });
  },
  onInputValue(e) {
    const { item } = e.currentTarget.dataset;
    const { value = '' } = e.detail;
    this.setData(
      {
        [`locationState.${item}`]: value,
      },
      () => {
        const { isLegal, tips } = this.onVerifyInputLegal();
        this.setData({ submitActive: isLegal });
        this.privateData.verifyTips = tips;
      },
    );
  },
  onCheckDefaultAddress({ detail }) {
    const { value } = detail;
    this.setData({
      'locationState.isDefault': value,
    });
  },

  onVerifyInputLegal() {
    const { name, phone, detailAddress, community } = this.data.locationState;
    const nameRegExp = new RegExp(innerNameReg);
    const phoneRegExp = new RegExp(innerPhoneReg);

    if (!name || !name.trim()) {
      return { isLegal: false, tips: '请填写联系人' };
    }
    if (!nameRegExp.test(name)) {
      return { isLegal: false, tips: '联系人仅支持中文、英文、数字' };
    }
    if (!phone || !phone.trim()) {
      return { isLegal: false, tips: '请填写手机号' };
    }
    if (!phoneRegExp.test(phone)) {
      return { isLegal: false, tips: '请填写正确的手机号' };
    }
    if (!community || !community.trim()) {
      return { isLegal: false, tips: '请填写社区自提点' };
    }
    if (!detailAddress || !detailAddress.trim()) {
      return { isLegal: false, tips: '请完善详细地址' };
    }
    if (detailAddress.trim().length > 50) {
      return { isLegal: false, tips: '详细地址不能超过50个字符' };
    }
    return { isLegal: true, tips: '添加成功' };
  },

  async formSubmit() {
    const { submitActive } = this.data;
    if (!submitActive) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: this.privateData.verifyTips,
        icon: '',
        duration: 1000,
      });
      return;
    }
    const { locationState } = this.data;
    const addressPayload = {
      id: locationState.addressId || undefined,
      addressId: locationState.addressId,
      phone: locationState.phone,
      name: locationState.name,
      community: locationState.community,
      detailAddr: locationState.detailAddress,
      detailAddress: locationState.detailAddress,
      isDefault: locationState.isDefault ? 1 : 0,
      addressTag: '自提点',
      provinceName: locationState.community,
      cityName: '',
      districtName: locationState.community,
      address: `${locationState.community}${locationState.detailAddress}`,
    };

    try {
      if (!config.useMock) {
        await saveDeliveryAddress(addressPayload);
      }
      this.hasSava = true;
      resolveAddress(addressPayload);
      wx.navigateBack({ delta: 1 });
    } catch (e) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: (e && e.message) || '保存失败',
        icon: '',
      });
    }
  },
});
