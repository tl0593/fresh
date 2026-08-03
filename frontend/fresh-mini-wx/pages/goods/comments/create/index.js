import Toast from 'tdesign-miniprogram/toast/index';
import { config } from '../../../../config/index';
import { getGoods, submitComment, uploadImage } from '../../../../services/order/orderSubmitComment';
import {
  isRemoteUploadedUrl,
  resolveUploadLocalPath,
} from '../../../../services/order/applyService';

const GOOD_TAGS = ['新鲜好吃', '性价比高', '会回购', '包装完好', '分量足'];
const BAD_TAGS = ['不够新鲜', '性价比低', '包装破损', '分量不足', '口感一般'];
const ONE_CLICK_CONTENT = '好评！新鲜又实惠，会回购。';

function toTagOptions(tags, selected = []) {
  const set = new Set(selected || []);
  return (tags || []).map((name) => ({ name, selected: set.has(name) }));
}

function selectedTagNames(tagOptions) {
  return (tagOptions || []).filter((t) => t.selected).map((t) => t.name);
}

function buildFormItem(goods) {
  return {
    orderItemId: goods.orderItemId,
    goodsId: goods.goodsId,
    title: goods.title || '商品',
    thumb: goods.thumb || '',
    specText: goods.specText || '',
    attitude: 'good',
    tagOptions: toTagOptions(GOOD_TAGS),
    content: '',
    uploadFiles: [],
    aiDeclared: false,
  };
}

Page({
  data: {
    orderNo: '',
    formList: [],
    canSubmit: false,
    submitting: false,
    gridConfig: {
      width: 160,
      height: 160,
      column: 4,
    },
    imageProps: {
      mode: 'aspectFill',
    },
  },

  async onLoad(options) {
    const orderNo = options.orderNo ? decodeURIComponent(options.orderNo) : '';
    this.setData({ orderNo });
    wx.setNavigationBarTitle({ title: '晒图评价' });
    await this.loadPendingGoods();
  },

  async loadPendingGoods() {
    const { orderNo } = this.data;
    if (!orderNo) return;
    try {
      const data = await getGoods({ orderNo });
      const pendingList = data.pendingList || [];
      if (!pendingList.length) {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '该订单商品均已评价',
          icon: '',
        });
        setTimeout(() => wx.navigateBack(), 800);
        return;
      }
      const formList = pendingList.map(buildFormItem);
      this.setData({ formList }, () => this.updateCanSubmit());
    } catch (e) {
      console.warn('[Fresh] 加载待评价商品失败', e && e.message);
      Toast({
        context: this,
        selector: '#t-toast',
        message: (e && e.message) || '加载商品失败',
        icon: '',
      });
    }
  },

  updateCanSubmit() {
    const { formList } = this.data;
    const canSubmit =
      formList.length > 0 &&
      formList.every((item) => {
        if (!item.attitude) return false;
        const text = (item.content || '').trim();
        return text.length > 0 || selectedTagNames(item.tagOptions).length > 0;
      });
    if (canSubmit !== this.data.canSubmit) {
      this.setData({ canSubmit });
    }
  },

  onAttitudeTap(e) {
    const { index, attitude } = e.currentTarget.dataset;
    const formList = this.data.formList.slice();
    const item = { ...formList[index] };
    if (!item || item.attitude === attitude) return;
    item.attitude = attitude;
    item.tagOptions = toTagOptions(attitude === 'bad' ? BAD_TAGS : GOOD_TAGS);
    formList[index] = item;
    this.setData({ formList }, () => this.updateCanSubmit());
  },

  onTagTap(e) {
    const { index, tag } = e.currentTarget.dataset;
    const formList = this.data.formList.slice();
    const item = { ...formList[index] };
    item.tagOptions = (item.tagOptions || []).map((t) =>
      t.name === tag ? { ...t, selected: !t.selected } : t,
    );
    formList[index] = item;
    this.setData({ formList }, () => this.updateCanSubmit());
  },

  onContentChange(e) {
    const index = e.currentTarget.dataset.index;
    const value = (e.detail && e.detail.value) || '';
    this.setData({ [`formList[${index}].content`]: value }, () => this.updateCanSubmit());
  },

  onAiDeclareChange(e) {
    const index = e.currentTarget.dataset.index;
    const checked = !!(e.detail && e.detail.checked);
    this.setData({ [`formList[${index}].aiDeclared`]: checked });
  },

  onOneClickGood() {
    const formList = (this.data.formList || []).map((item) => ({
      ...item,
      attitude: 'good',
      tagOptions: toTagOptions(GOOD_TAGS, GOOD_TAGS.slice(0, 3)),
      content: item.content && item.content.trim() ? item.content : ONE_CLICK_CONTENT,
    }));
    this.setData({ formList }, () => this.updateCanSubmit());
    Toast({
      context: this,
      selector: '#t-toast',
      message: '已一键好评，可再修改',
      icon: 'check-circle',
    });
  },

  normalizeUploadFiles(files) {
    const list = files || [];
    const seen = new Set();
    const result = [];
    list.forEach((f) => {
      const key = resolveUploadLocalPath(f) || (typeof f === 'string' ? f : '');
      if (!key || seen.has(key)) return;
      seen.add(key);
      result.push(f);
    });
    return result;
  },

  resolveUploadIndex(e) {
    const fromTarget = e && e.currentTarget && e.currentTarget.dataset;
    if (fromTarget && fromTarget.index !== undefined && fromTarget.index !== '') {
      return Number(fromTarget.index);
    }
    return -1;
  },

  handleAdd(e) {
    const index = this.resolveUploadIndex(e);
    if (index < 0) return;
    const files = this.normalizeUploadFiles((e.detail || {}).files);
    this.setData({ [`formList[${index}].uploadFiles`]: files });
  },

  handleSuccess(e) {
    const index = this.resolveUploadIndex(e);
    if (index < 0) return;
    const files = this.normalizeUploadFiles((e.detail || {}).files);
    this.setData({ [`formList[${index}].uploadFiles`]: files });
  },

  handleRemove(e) {
    const index = this.resolveUploadIndex(e);
    if (index < 0) return;
    const { index: fileIndex } = e.detail || {};
    const uploadFiles = ((this.data.formList[index] && this.data.formList[index].uploadFiles) || []).slice();
    if (typeof fileIndex === 'number' && fileIndex >= 0) {
      uploadFiles.splice(fileIndex, 1);
      this.setData({
        [`formList[${index}].uploadFiles`]: this.normalizeUploadFiles(uploadFiles),
      });
    }
  },

  buildContent(item) {
    const tags = selectedTagNames(item.tagOptions).join('，');
    const text = (item.content || '').trim();
    let content = text;
    if (tags) {
      content = text ? `${tags}。${text}` : tags;
    }
    if (item.aiDeclared) {
      content = content ? `${content}（含AI生成内容）` : '（含AI生成内容）';
    }
    return content;
  },

  async collectImages(uploadFiles) {
    const images = [];
    const seen = new Set();
    for (const f of uploadFiles || []) {
      const localPath = resolveUploadLocalPath(f);
      if (!localPath || seen.has(localPath)) continue;
      seen.add(localPath);
      if (isRemoteUploadedUrl(localPath) && /\.(png|jpe?g|webp|gif)(\?|$)/i.test(localPath)) {
        images.push(localPath.replace(/\/(afterSale|comment|goods),\1\//g, '/$1/'));
        continue;
      }
      const uploaded = await uploadImage(localPath, 'comment');
      const url = typeof uploaded === 'string' ? uploaded : (uploaded && uploaded.url) || '';
      if (!url || !isRemoteUploadedUrl(url)) {
        throw new Error('图片上传失败，请重试');
      }
      images.push(url.replace(/\/(afterSale|comment|goods),\1\//g, '/$1/'));
    }
    return images;
  },

  markPrevRefresh() {
    const pages = getCurrentPages();
    const prev = pages[pages.length - 2];
    if (prev) {
      prev.setData({ backRefresh: true });
    }
  },

  async onSubmitBtnClick() {
    const { canSubmit, submitting, formList } = this.data;
    if (!canSubmit || submitting) return;

    this.setData({ submitting: true });
    try {
      if (!config.useMock) {
        for (const item of formList) {
          const content = this.buildContent(item);
          if (!content) {
            throw new Error(`请填写「${item.title}」的评价`);
          }
          const images = await this.collectImages(item.uploadFiles);
          await submitComment({
            orderItemId: Number(item.orderItemId),
            score: item.attitude === 'bad' ? 1 : 5,
            content,
            images,
          });
        }
      }
      this.markPrevRefresh();
      Toast({
        context: this,
        selector: '#t-toast',
        message: '评价提交成功',
        icon: 'check-circle',
      });
      setTimeout(() => wx.navigateBack(), 800);
    } catch (e) {
      Toast({
        context: this,
        selector: '#t-toast',
        message: (e && e.message) || '提交失败',
        icon: '',
      });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
