import { fetchComments, fetchMyComments } from '../../../services/comments/fetchComments';
import { fetchCommentsCount } from '../../../services/comments/fetchCommentsCount';
import dayjs from 'dayjs';

const layoutMap = {
  0: 'vertical',
};

function formatCommentTime(raw) {
  if (raw == null || raw === '') return '';
  if (typeof raw === 'number' || /^\d+$/.test(String(raw))) {
    return dayjs(Number(raw)).format('YYYY/MM/DD HH:mm');
  }
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY/MM/DD HH:mm') : String(raw);
}

function filterByTag(list, { commentLevel, hasImage }) {
  let rows = list || [];
  if (hasImage === '1') {
    rows = rows.filter((item) => (item.commentResources || item.commentImageList || []).length > 0);
  }
  const level = Number(commentLevel);
  if (level === 3) rows = rows.filter((item) => Number(item.commentScore) >= 4);
  else if (level === 2) rows = rows.filter((item) => Number(item.commentScore) === 3);
  else if (level === 1) rows = rows.filter((item) => Number(item.commentScore) > 0 && Number(item.commentScore) <= 2);
  return rows;
}

Page({
  data: {
    pageLoading: false,
    commentList: [],
    pageNum: 1,
    myPageNum: 1,
    pageSize: 10,
    total: 0,
    myTotal: 0,
    hasLoaded: false,
    layoutText: layoutMap[0],
    loadMoreStatus: 0,
    myLoadStatus: 0,
    spuId: '',
    commentLevel: '',
    hasImage: '',
    commentType: '',
    totalCount: 0,
    countObj: {
      badCount: '0',
      commentCount: '0',
      goodCount: '0',
      middleCount: '0',
      hasImageCount: '0',
      uidCount: '0',
    },
  },
  onLoad(options) {
    this.getCount(options);
    this.getComments(options);
  },
  async getCount(options) {
    try {
      const result = await fetchCommentsCount({ spuId: options.spuId });
      this.setData({ countObj: result });
      wx.setNavigationBarTitle({
        title: `全部评价(${result.commentCount || 0})`,
      });
    } catch (error) {
      /* ignore */
    }
  },
  generalQueryData(reset) {
    const { pageNum, pageSize, spuId } = this.data;
    const params = {
      pageNum: 1,
      pageSize: 50,
      spuId,
      queryParameter: { spuId },
    };
    if (reset) return params;
    return { ...params, pageNum: pageNum + 1, pageSize };
  },
  async init(reset = true) {
    const { loadMoreStatus, commentList = [], commentLevel, hasImage } = this.data;
    const params = this.generalQueryData(reset);

    if (loadMoreStatus !== 0) return;

    this.setData({ loadMoreStatus: 1 });

    try {
      const data = await fetchComments(params);
      let pageList = (data && data.pageList) || [];
      pageList = filterByTag(pageList, { commentLevel, hasImage });
      pageList.forEach((item) => {
        // eslint-disable-next-line no-param-reassign
        item.commentTime = formatCommentTime(item.commentTime);
      });
      const totalCount = pageList.length;
      if (Number(totalCount) === 0 && reset) {
        this.setData({
          commentList: [],
          hasLoaded: true,
          total: 0,
          totalCount: 0,
          loadMoreStatus: 2,
        });
        return;
      }
      const _commentList = reset ? pageList : commentList.concat(pageList);
      this.setData({
        commentList: _commentList,
        pageNum: params.pageNum || 1,
        total: _commentList.length,
        totalCount: _commentList.length,
        loadMoreStatus: 2,
      });
    } catch (error) {
      wx.showToast({ title: '查询失败，请稍候重试', icon: 'none' });
      this.setData({ loadMoreStatus: 3 });
    }
    this.setData({ hasLoaded: true });
  },
  async getMyCommentsList() {
    try {
      this.setData({ loadMoreStatus: 1 });
      const data = await fetchMyComments(this.data.spuId);
      const pageList = (data.pageList || []).map((item) => ({
        ...item,
        commentTime: formatCommentTime(item.commentTime),
      }));
      this.setData({
        commentList: pageList,
        total: pageList.length,
        totalCount: pageList.length,
        myTotal: pageList.length,
        loadMoreStatus: 2,
        myLoadStatus: 2,
        hasLoaded: true,
      });
    } catch (e) {
      wx.showToast({ title: '加载我的评价失败', icon: 'none' });
      this.setData({
        commentList: [],
        loadMoreStatus: 3,
        myLoadStatus: 3,
        hasLoaded: true,
      });
    }
  },
  getComments(options) {
    const { commentLevel = -1, spuId, hasImage = '' } = options || {};
    if (commentLevel !== -1) {
      this.setData({ commentLevel });
    }
    this.setData({
      hasImage,
      commentType: hasImage ? '4' : '',
      spuId: spuId || '',
    });
    this.init(true);
  },
  changeTag(e) {
    const { commenttype } = e.currentTarget.dataset;
    const { commentType } = this.data;
    if (commentType === commenttype) return;
    this.setData({
      loadMoreStatus: 0,
      commentList: [],
      total: 0,
      myTotal: 0,
      myPageNum: 1,
      pageNum: 1,
    });
    if (commenttype === '' || commenttype === '5') {
      this.setData({ hasImage: '', commentLevel: '' });
    } else if (commenttype === '4') {
      this.setData({ hasImage: '1', commentLevel: '' });
    } else {
      this.setData({ hasImage: '', commentLevel: commenttype });
    }
    if (commenttype === '5') {
      this.setData({ myLoadStatus: 1, commentType: commenttype });
      this.getMyCommentsList();
    } else {
      this.setData({ myLoadStatus: 0, commentType: commenttype });
      this.init(true);
    }
  },
  onReachBottom() {
    /* 当前一次拉取足量，无需分页追加 */
  },
});
