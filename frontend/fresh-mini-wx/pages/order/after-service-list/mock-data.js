/* eslint-disable */
/** Mock 售后列表（仅 config.useMock=true 时使用）。保持精简，避免编码损坏导致编译失败。 */
import { mockIp, mockReqId } from '../../../utils/mock';

export const resp = {
  data: {
    pageNum: 1,
    pageSize: 10,
    totalCount: 1,
    states: {
      audit: 1,
      approved: 0,
      complete: 0,
      closed: 0,
    },
    dataList: [
      {
        buttonVOs: [],
        storeId: '1',
        rights: {
          bizRightsStatus: 1,
          bizRightsStatusName: '待审核',
          createTime: '2026-08-01 12:00:00',
          orderNo: 'MOCK_ORDER_001',
          refundAmount: 1000,
          refundRequestAmount: 1000,
          rightsNo: 'MOCK_RIGHTS_001',
          rightsStatus: 10,
          rightsStatusName: '待审核',
          rightsType: 20,
          storeName: '社区自提点',
          userRightsStatusName: '待商家审核',
          userRightsStatusDesc: '售后申请已提交，请等待商家审核处理',
          rightsReasonDesc: '商品破损',
          rightsImageUrls: [],
        },
        rightsItem: [
          {
            goodsName: '示例生鲜商品',
            goodsPictureUrl: '',
            rightsQuantity: 1,
            itemRefundAmount: 1000,
            refundAmount: 1000,
            specInfo: [],
          },
        ],
        logisticsVO: {},
      },
    ],
  },
  code: 'Success',
  success: true,
  requestId: mockReqId(),
  clientIp: mockIp(),
};
