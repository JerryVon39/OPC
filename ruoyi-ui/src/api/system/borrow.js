import request from '@/utils/request'

// 查询借阅记录列表
export function listBorrow(query) {
  return request({ url: '/system/borrow/list', method: 'get', params: query })
}

// 查询借阅记录详细
export function getBorrow(borrowId) {
  return request({ url: '/system/borrow/' + borrowId, method: 'get' })
}

// 新增借阅记录（借书）
export function addBorrow(data) {
  return request({ url: '/system/borrow', method: 'post', data: data })
}

// 修改借阅记录
export function updateBorrow(data) {
  return request({ url: '/system/borrow', method: 'put', data: data })
}

// 还书
export function returnBorrow(borrowId) {
  return request({ url: '/system/borrow/return/' + borrowId, method: 'put' })
}

// 续借
export function renewBorrow(borrowId) {
  return request({ url: '/system/borrow/renew/' + borrowId, method: 'put' })
}

// 罚款收款（缴纳逾期罚款）
export function payFine(borrowId) {
  return request({ url: '/system/borrow/payFine/' + borrowId, method: 'put' })
}

// 借阅统计·热门图书（匿名接口，前台热门推荐用）
export function borrowStats() {
  return request({ url: '/system/borrow/stats', method: 'get' })
}

// 借阅统计·读者排行（含证号，需 system:borrow:stats 权限）
export function borrowReaderStats() {
  return request({ url: '/system/borrow/stats/readers', method: 'get' })
}

// 删除借阅记录
export function delBorrow(borrowId) {
  return request({ url: '/system/borrow/' + borrowId, method: 'delete' })
}
