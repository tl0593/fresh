/* eslint-disable */
import { mockIp, mockReqId } from '../../../utils/mock';

/** mock 大数据单独文件，避免打开售后页时同步解析卡住 */
export function getMockResp() {
  return require('./mock-data').resp;
}

function yuanToFen(yuan) {
  const n = Number(yuan);
  if (Number.isNaN(n)) return 0;
  return Math.round(n * 100);
}

/** auditStatus: 0待审 1通过 2驳回 -> 列表展示状态 */
function mapAuditToRights(as) {
  const audit = Number(as.auditStatus);
  let rightsStatus = 10;
  let rightsStatusName = '待审核';
  let userRightsStatusName = '待商家审核';
  let userRightsStatusDesc = '售后申请已提交，请等待商家审核处理';
  if (audit === 1) {
    rightsStatus = 50;
    rightsStatusName = '已完成';
    userRightsStatusName = '退款已处理';
    userRightsStatusDesc = '商家已通过售后，退款将按原路返回';
  } else if (audit === 2) {
    rightsStatus = 60;
    rightsStatusName = '已关闭';
    userRightsStatusName = '售后已驳回';
    userRightsStatusDesc = '商家已驳回本次售后申请';
  }
  const refundFen = yuanToFen(
    as.actualRefundMoney != null ? as.actualRefundMoney : as.aiRefundMoney != null ? as.aiRefundMoney : as.itemPrice,
  );
  const rightsNo = String(as.id != null ? as.id : as.orderNo || '');
  return {
    buttonVOs: [],
    createTime: as.createTime,
    storeId: '1',
    rights: {
      bizRightsStatus: 1,
      bizRightsStatusName: rightsStatusName,
      createTime: as.createTime,
      orderNo: as.orderNo,
      refundAmount: refundFen,
      refundRequestAmount: refundFen,
      rightsNo,
      rightsStatus,
      rightsStatusName,
      rightsType: 20,
      storeName: '社区自提点',
      userRightsStatusName,
      userRightsStatusDesc,
      rightsReasonDesc: as.remark || '',
      rightsImageUrls: as.damageImg
        ? String(as.damageImg)
            .split('|')
            .map((s) => s.trim())
            .filter(Boolean)
        : [],
    },
    rightsItem: [
      {
        goodsName: as.goodsName || '生鲜商品',
        goodsPictureUrl: as.goodsImg || '',
        rightsQuantity: as.itemNum || 1,
        itemRefundAmount: refundFen,
        refundAmount: refundFen,
        specInfo: [],
      },
    ],
    logisticsVO: {},
  };
}

export async function getRightsList({ parameter: { afterServiceStatus, pageNum } = {} } = {}) {
  const { config } = require('../../../config/index');
  if (config.useMock) {
    const resp = getMockResp();
    const _resq = JSON.parse(JSON.stringify(resp));
    if (pageNum > 3) _resq.data.dataList = [];
    if (afterServiceStatus > -1) {
      _resq.data.dataList = _resq.data.dataList.filter(
        (item) => item.rights.rightsStatus === afterServiceStatus,
      );
    }
    return _resq;
  }

  const { ensureLogin } = require('../../../services/auth/login');
  const request = require('../../../utils/request').default;
  await ensureLogin();

  let rows = [];
  try {
    rows = (await request.get('/api/order/afterSale/mine')) || [];
  } catch (e) {
    console.warn('[Fresh] 售后列表接口失败，回退订单状态', e && e.message);
    // 兼容旧后端：仅展示 status=4
    const list = (await request.get('/api/order/order/list')) || [];
    rows = list
      .filter((d) => Number(((d && d.order) || d || {}).status) === 4)
      .map((d) => {
        const o = (d && d.order) || d || {};
        const items = (d && d.items) || [];
        const it = items[0] || {};
        return {
          id: o.id,
          orderNo: o.orderNo,
          goodsName: it.goodsName,
          goodsImg: it.goodsImg,
          itemPrice: o.payAmount,
          itemNum: it.num || 1,
          auditStatus: 0,
          createTime: o.createTime,
          remark: '',
          damageImg: '',
        };
      });
  }

  let dataList = (rows || []).map(mapAuditToRights);
  if (afterServiceStatus != null && afterServiceStatus > -1) {
    dataList = dataList.filter((item) => item.rights.rightsStatus === afterServiceStatus);
  }

  const states = {
    audit: 0,
    approved: 0,
    complete: 0,
    closed: 0,
  };
  dataList.forEach((item) => {
    const s = item.rights.rightsStatus;
    if (s === 10) states.audit += 1;
    else if (s === 20) states.approved += 1;
    else if (s === 50) states.complete += 1;
    else if (s === 60) states.closed += 1;
  });

  return {
    data: {
      pageNum: 1,
      pageSize: 50,
      totalCount: dataList.length,
      states,
      dataList,
    },
    code: 'Success',
    success: true,
    requestId: mockReqId(),
    clientIp: mockIp(),
  };
}
