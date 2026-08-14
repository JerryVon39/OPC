import request from '@/utils/request'

// 查询预约列表
export function listReserve(query) {
  return request({ url: '/system/reserve/list', method: 'get', params: query })
}

// 删除预约
export function delReserve(reserveId) {
  return request({ url: '/system/reserve/' + reserveId, method: 'delete' })
}
