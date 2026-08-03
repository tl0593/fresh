let ARRAY = [];
Component({
  externalClasses: ['wr-class'],

  options: {
    multipleSlots: true,
  },
  properties: {
    disabled: Boolean,
    leftWidth: {
      type: Number,
      value: 0,
    },
    rightWidth: {
      type: Number,
      value: 0,
    },
    asyncClose: Boolean,
  },
  attached() {
    ARRAY.push(this);
  },

  detached() {
    ARRAY = ARRAY.filter((item) => item !== this);
  },

  data: {
    wrapperStyle: '',
    asyncClose: false,
    closed: true,
  },

  methods: {
    open(position) {
      this.setData({ closed: false });
      this.triggerEvent('close', {
        position,
        instance: this,
      });
    },

    close() {
      this.setData({ closed: true });
    },

    closeOther() {
      ARRAY.filter((item) => item !== this).forEach((item) => item.close());
    },

    onContentTap() {
      if (!this.data.closed) {
        this.close();
      }
    },
  },
});
