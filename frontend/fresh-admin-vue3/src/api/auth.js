import request from '@/utils/request'

export function adminLogin(data) {
  return request.post('/api/user/admin/login', data)
}
