import request from '@/utils/request'

// ================= 图书回收站 =================

// 查询回收站图书列表
export function listRecycleBook(query) {
  return request({
    url: '/system/recycle/book/list',
    method: 'get',
    params: query
  })
}

// 查询回收站图书数量
export function countRecycleBook() {
  return request({
    url: '/system/recycle/book/count',
    method: 'get'
  })
}

// 还原回收站图书（可多个）
export function restoreRecycleBook(recycleIds) {
  return request({
    url: '/system/recycle/book/restore/' + recycleIds,
    method: 'put'
  })
}

// 彻底删除回收站图书
export function delRecycleBook(recycleIds) {
  return request({
    url: '/system/recycle/book/' + recycleIds,
    method: 'delete'
  })
}

// 清空回收站图书
export function clearRecycleBook() {
  return request({
    url: '/system/recycle/book/clear',
    method: 'delete'
  })
}

// ================= 读者回收站 =================

// 查询回收站读者列表
export function listRecycleReader(query) {
  return request({
    url: '/system/recycle/reader/list',
    method: 'get',
    params: query
  })
}

// 查询回收站读者数量
export function countRecycleReader() {
  return request({
    url: '/system/recycle/reader/count',
    method: 'get'
  })
}

// 还原回收站读者（可多个）
export function restoreRecycleReader(recycleIds) {
  return request({
    url: '/system/recycle/reader/restore/' + recycleIds,
    method: 'put'
  })
}

// 彻底删除回收站读者
export function delRecycleReader(recycleIds) {
  return request({
    url: '/system/recycle/reader/' + recycleIds,
    method: 'delete'
  })
}

// 清空回收站读者
export function clearRecycleReader() {
  return request({
    url: '/system/recycle/reader/clear',
    method: 'delete'
  })
}