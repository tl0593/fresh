Component({
  options: {
    addGlobalClass: true,
  },
  properties: {
    isAllSelected: {
      type: Boolean,
      value: false,
    },
    totalAmount: {
      type: Number,
      value: 1,
    },
    totalGoodsNum: {
      type: Number,
      value: 0,
      observer(num) {
        const isDisabled = num == 0;
        setTimeout(() => {
          this.setData({
            isDisabled,
          });
        });
      },
    },
    totalDiscountAmount: {
      type: Number,
      value: 0,
    },
    bottomHeight: {
      type: Number,
      value: 100,
    },
    fixed: Boolean,
    isManageMode: {
      type: Boolean,
      value: false,
    },
  },
  data: {
    isDisabled: false,
  },

  methods: {
    handleSelectAll() {
      const { isAllSelected } = this.data;
      this.setData({
        isAllSelected: !isAllSelected,
      });
      this.triggerEvent('handleSelectAll', {
        isAllSelected: isAllSelected,
      });
    },

    handleToSettle() {
      if (this.data.isDisabled) return;
      this.triggerEvent('handleToSettle');
    },

    handleDeleteSelected() {
      if (this.data.isDisabled) return;
      this.triggerEvent('handleDeleteSelected');
    },
  },
});
