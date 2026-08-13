import request from '@/utils/request'

// 查询订单列表
export function listOrder(query) {
  return request({ url: '/system/order/list', method: 'get', params: query })
}

// 修改订单（状态流转）
export function updateOrder(data) {
  return request({ url: '/system/order', method: 'put', data: data })
}

// 删除订单
export function delOrder(orderId) {
  return request({ url: '/system/order/' + orderId, method: 'delete' })
}
