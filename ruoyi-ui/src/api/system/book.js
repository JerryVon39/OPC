import request from '@/utils/request'

// 查询图书信息列表
export function listBook(query) {
  return request({
    url: '/system/book/list',
    method: 'get',
    params: query
  })
}

// 查询图书信息详细
export function getBook(bookId) {
  return request({
    url: '/system/book/' + bookId,
    method: 'get'
  })
}

// 新增图书信息
export function addBook(data) {
  return request({
    url: '/system/book',
    method: 'post',
    data: data
  })
}

// 修改图书信息
export function updateBook(data) {
  return request({
    url: '/system/book',
    method: 'put',
    data: data
  })
}

// 上下架状态切换（后台列表开关）
export function changeBookStatus(bookId, status) {
  return request({
    url: '/system/book/changeStatus',
    method: 'put',
    params: { bookId: bookId, status: status }
  })
}

// 删除图书信息
export function delBook(bookId) {
  return request({
    url: '/system/book/' + bookId,
    method: 'delete'
  })
}

// 查询已删除图书列表（后台回收站视图）
export function listDeletedBook(query) {
  return request({
    url: '/system/book/deletedList',
    method: 'get',
    params: query
  })
}

// 恢复已删除图书
export function restoreBook(bookIds) {
  return request({
    url: '/system/book/restore/' + bookIds,
    method: 'put'
  })
}

// 永久删除图书
export function purgeBook(bookIds) {
  return request({
    url: '/system/book/purge/' + bookIds,
    method: 'delete'
  })
}
