const emptyCouponImg = `https://tdesign.gtimg.com/miniprogram/template/retail/coupon/ordersure-coupon-newempty.png`;

Component({
  properties: {
    storeId: {
      type: null,
      value: '1',
    },
    availableCoupons: {
      type: Array,
      value: [],
    },
    selectedCouponId: {
      type: null,
      value: null,
    },
    couponsShow: {
      type: Boolean,
      value: false,
      observer(couponsShow) {
        if (couponsShow) {
          this.initFromAvailable();
        }
      },
    },
  },
  data: {
    emptyCouponImg,
    selectedList: [],
    couponsList: [],
    selectedNum: 0,
    reduce: 0,
  },
  methods: {
    initFromAvailable() {
      const { availableCoupons = [], selectedCouponId } = this.properties;
      let selectedNum = 0;
      let reduce = 0;
      const selectedList = [];
      const couponsList = (availableCoupons || []).map((coupon) => {
        const couponId = coupon.couponId || coupon.key;
        const isSelected =
          selectedCouponId != null
            ? Number(selectedCouponId) === Number(couponId)
            : !!coupon.isSelected;
        if (isSelected) {
          selectedNum += 1;
          reduce = Number(coupon.value || 0);
          selectedList.push({
            couponId: Number(couponId),
            storeId: this.properties.storeId || '1',
            value: coupon.value,
            type: 1,
            status: 'default',
          });
        }
        return {
          key: String(couponId),
          title: coupon.title || '优惠券',
          isSelected,
          timeLimit: coupon.timeLimit || '',
          // ui-coupon-card 满减券 value 按分展示
          value: Number(coupon.value || 0),
          status: coupon.status || 'default',
          desc: coupon.desc || '',
          type: 1,
          tag: '',
        };
      });
      this.setData({
        couponsList,
        selectedList,
        selectedNum,
        reduce,
      });
    },
    selectCoupon(e) {
      const { key } = e.currentTarget.dataset;
      const couponsList = (this.data.couponsList || []).map((coupon) => {
        const nextSelected = coupon.key === key ? !coupon.isSelected : false;
        return { ...coupon, isSelected: nextSelected };
      });
      const selected = couponsList.filter((c) => c.isSelected);
      const selectedList = selected.map((c) => ({
        couponId: Number(c.key),
        storeId: this.properties.storeId || '1',
        value: c.value,
        type: 1,
        status: 'default',
      }));
      this.setData({
        couponsList,
        selectedList,
        selectedNum: selected.length,
        reduce: selected[0] ? selected[0].value : 0,
      });
      // 选中/取消后立即回写结算金额
      this.triggerEvent('sure', { selectedList });
    },
    hide() {
      this.setData({
        couponsShow: false,
      });
      this.triggerEvent('close');
    },
  },
});
