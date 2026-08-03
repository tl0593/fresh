import request from '@/utils/request'

// 分类
export const categoryList = () => request.get('/api/goods/admin/category/list')
export const categorySave = (data) => request.post('/api/goods/admin/category/save', data)
export const categoryDelete = (id) => request.delete(`/api/goods/admin/category/${id}`)

// 商品
export const goodsList = () => request.get('/api/goods/admin/goods/list')
export const goodsDetail = (id) => request.get(`/api/goods/admin/goods/${id}`)
export const goodsSave = (data) => request.post('/api/goods/admin/goods/save', data)
export const goodsSaveWithSpecs = (data) => request.post('/api/goods/admin/goods/saveWithSpecs', data)
export const goodsDelete = (id) => request.delete(`/api/goods/admin/goods/${id}`)
export const specSave = (data) => request.post('/api/goods/admin/goods/spec/save', data)
export const specDelete = (id) => request.delete(`/api/goods/admin/goods/spec/${id}`)
export const specListByGoods = (goodsId) => request.get(`/api/goods/admin/goods/${goodsId}/specs`)

// 补货提醒
export const stockAlertList = (params) => request.get('/api/goods/admin/stock/alert', { params })
export const stockAlertCount = (params) => request.get('/api/goods/admin/stock/alertCount', { params, silent: true })
export const stockRestock = (data) => request.post('/api/goods/admin/stock/restock', data)

// 团购
export const groupList = () => request.get('/api/goods/admin/group/list')
export const groupSave = (data) => request.post('/api/goods/admin/group/save', data)
export const groupDelete = (id) => request.delete(`/api/goods/admin/group/${id}`)

// 秒杀
export const seckillList = () => request.get('/api/goods/admin/seckill/list')
export const seckillSave = (data) => request.post('/api/goods/admin/seckill/save', data)
export const seckillDelete = (id) => request.delete(`/api/goods/admin/seckill/${id}`)

// 评价
export const commentPage = (params) => request.get('/api/goods/admin/comment/page', { params })
export const commentHide = (id) => request.put(`/api/goods/admin/comment/hide/${id}`)
export const commentReply = (data) => request.post('/api/goods/admin/comment/reply', data)

// 优惠券
export const couponList = () => request.get('/api/goods/admin/coupon/list')
export const couponSave = (data) => request.post('/api/goods/admin/coupon/save', data)
export const couponDelete = (id) => request.delete(`/api/goods/admin/coupon/${id}`)
export const couponLog = (params) => request.get('/api/goods/admin/coupon/log', { params })

// 满减
export const fullReduceList = () => request.get('/api/goods/admin/fullreduce/list')
export const fullReduceSave = (data) => request.post('/api/goods/admin/fullreduce/save', data)
export const fullReduceDelete = (id) => request.delete(`/api/goods/admin/fullreduce/${id}`)

// 积分兑券
export const integralCouponList = () => request.get('/api/goods/admin/integralCoupon/list')
export const integralCouponSave = (data) => request.post('/api/goods/admin/integralCoupon/save', data)
export const integralCouponDelete = (id) => request.delete(`/api/goods/admin/integralCoupon/${id}`)

// 整点抢券
export const seckillCouponList = () => request.get('/api/goods/admin/seckillCoupon/list')
export const seckillCouponSave = (data) => request.post('/api/goods/admin/seckillCoupon/save', data)
export const seckillCouponDelete = (id) => request.delete(`/api/goods/admin/seckillCoupon/${id}`)

// 抽奖
export const lotteryList = () => request.get('/api/goods/admin/lotteryPrize/list')
export const lotterySave = (data) => request.post('/api/goods/admin/lotteryPrize/save', data)
export const lotteryDelete = (id) => request.delete(`/api/goods/admin/lotteryPrize/${id}`)

// 上传（不要手动设 Content-Type，否则会缺少 boundary 导致解析失败）
export function uploadImage(file, dir = 'goods') {
  const form = new FormData()
  form.append('file', file)
  form.append('dir', dir)
  return request.post('/api/goods/upload/image', form)
}
