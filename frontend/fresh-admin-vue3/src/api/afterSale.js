import request from '@/utils/request'

export const afterSaleList = (params) => request.get('/api/order/admin/afterSale/page', { params })
export const afterSalePendingCount = () => request.get('/api/order/admin/afterSale/pendingCount', { silent: true })
export const afterSaleAudit = (data) => request.post('/api/order/admin/afterSale/audit', data)
