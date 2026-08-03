import request from '@/utils/request'

export const orderList = (params) => request.get('/api/order/admin/order/list', { params })
export const orderDetail = (orderNo) => request.get(`/api/order/admin/order/${orderNo}`)
export const orderComplete = (orderNo) => request.post(`/api/order/admin/order/${orderNo}/complete`)
export const orderArrive = (orderNo) => request.post(`/api/order/admin/order/${orderNo}/arrive`)
