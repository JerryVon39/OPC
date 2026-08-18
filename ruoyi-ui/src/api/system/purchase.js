import request from '@/utils/request'

// 查询荐购申请列表
export function listPurchase(query) {
  return request({
    url: '/system/purchase/list',
    method: 'get',
    params: query
  })
}

// 查询荐购申请详细
export function getPurchase(reqId) {
  return request({
    url: '/system/purchase/' + reqId,
    method: 'get'
  })
}

// 处理荐购申请（标记已处理/已拒绝）
export function updatePurchase(data) {
  return request({
    url: '/system/purchase',
    method: 'put',
    data: data
  })
}

// 删除荐购申请
export function delPurchase(reqId) {
  return request({
    url: '/system/purchase/' + reqId,
    method: 'delete'
  })
}