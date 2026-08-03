import request from '@/utils/request'

export const getTodayStat = () => request.get('/api/data/stat/today')
export const getDailyList = (params) => request.get('/api/data/stat/daily/list', { params })
export const getGoodsSales = (params) => request.get('/api/data/stat/goods/sales', { params })
export const getUserTrend = (params) => request.get('/api/data/stat/user/trend', { params })
export const getGroupRate = (params) => request.get('/api/data/stat/group/rate', { params })