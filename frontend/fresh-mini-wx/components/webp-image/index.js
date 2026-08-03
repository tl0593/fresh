import { toDisplayableImage, isLocalHttpUrl, getImagePlaceholder } from '../../utils/localImage';

const systemInfo = wx.getSystemInfoSync();

function isTempLocalPath(url) {
  const src = (url || '').trim();
  if (!src) return false;
  return (
    src.indexOf('wxfile://') === 0 ||
    src.indexOf('http://tmp') === 0 ||
    src.indexOf('http://usr') === 0 ||
    (typeof wx !== 'undefined' &&
      wx.env &&
      wx.env.USER_DATA_PATH &&
      src.indexOf(wx.env.USER_DATA_PATH) === 0)
  );
}

Component({
  options: {
    // 不用 virtualHost：否则子组件 bind:error 会报找不到 handleImageError
    multipleSlots: true,
  },
  externalClasses: ['t-class', 't-class-load'],
  properties: {
    loadFailed: {
      type: String,
      value: 'default',
    },
    loading: {
      type: String,
      value: 'default',
    },
    src: {
      type: String,
      value: '',
    },
    mode: {
      type: String,
      value: 'aspectFill',
    },
    webp: {
      type: Boolean,
      value: true,
    },
    lazyLoad: {
      type: Boolean,
      value: false,
    },
    showMenuByLongpress: {
      type: Boolean,
      value: false,
    },
  },
  data: {
    thumbHeight: 375,
    thumbWidth: 375,
    systemInfo,
    displaySrc: '',
    useNative: false,
  },
  observers: {
    src(url) {
      this.resolveSrc(url);
    },
  },
  lifetimes: {
    attached() {
      this.resolveSrc(this.properties.src);
    },
    ready() {
      const { mode } = this.properties;
      this.getRect('.J-image').then((res) => {
        if (!res) return;
        const { width, height } = res;
        this.setData(
          mode === 'heightFix'
            ? { thumbHeight: this.px2rpx(height) || 375 }
            : { thumbWidth: this.px2rpx(width) || 375 },
        );
      });
    },
  },
  methods: {
    resolveSrc(url) {
      this._reqSeq = (this._reqSeq || 0) + 1;
      const seq = this._reqSeq;
      const src = (url || '').trim();
      this._fallbackUsed = false;

      if (isTempLocalPath(src)) {
        this.setData({ useNative: true, displaySrc: src });
        return;
      }

      if (!isLocalHttpUrl(src)) {
        this.setData({ useNative: false, displaySrc: src });
        return;
      }

      // 本地网关 http 上传图：下载到临时文件再展示（失败回退占位，避免空白）
      this.setData({ useNative: true, displaySrc: '' });
      toDisplayableImage(src)
        .then((localPath) => {
          if (seq !== this._reqSeq) return;
          if (localPath) {
            this.setData({ useNative: true, displaySrc: localPath });
            return;
          }
          this.setData({ useNative: false, displaySrc: getImagePlaceholder() });
        })
        .catch(() => {
          if (seq !== this._reqSeq) return;
          this.setData({ useNative: false, displaySrc: getImagePlaceholder() });
        });
    },

    px2rpx(px) {
      return (750 / (systemInfo.screenWidth || 375)) * px;
    },
    getRect(selector) {
      return new Promise((resolve) => {
        this.createSelectorQuery()
          .select(selector)
          .boundingClientRect(resolve)
          .exec();
      });
    },
    handleImageLoad(e) {
      this.triggerEvent('load', e.detail);
    },
    handleImageError(e) {
      if (!this._fallbackUsed) {
        this._fallbackUsed = true;
        this.setData({ useNative: false, displaySrc: getImagePlaceholder() });
      }
      this.triggerEvent('error', e.detail);
    },
  },
});
