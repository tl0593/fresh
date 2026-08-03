import request from '@/utils/request'

export const templateList = () => request.get('/api/message/template/list')
