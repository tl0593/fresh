import request from '@/utils/request'

export const knowledgeList = () => request.get('/api/ai/ai/knowledge/list')
export const knowledgeSave = (data) => request.post('/api/ai/ai/knowledge/save', data)
export const knowledgeUpdate = (data) => request.put('/api/ai/ai/knowledge/update', data)
export const knowledgeDelete = (id) => request.delete(`/api/ai/ai/knowledge/${id}`)
export const generateGroupText = (data) => request.post('/api/ai/ai/group/text/generate', data)

export const chatLogPage = (params) => request.get('/api/ai/ai/chat/log/page', { params })
export const imageLogPage = (params) => request.get('/api/ai/ai/image/rec/log/page', { params })
export const groupTextLogPage = (params) => request.get('/api/ai/ai/group/text/log/page', { params })
