import request from '@/utils/request'

// 查询读者管理列表
export function listReader(query) {
  return request({
    url: '/system/reader/list',
    method: 'get',
    params: query
  })
}

// 查询读者管理详细
export function getReader(readerId) {
  return request({
    url: '/system/reader/' + readerId,
    method: 'get'
  })
}

// 新增读者管理
export function addReader(data) {
  return request({
    url: '/system/reader',
    method: 'post',
    data: data
  })
}

// 修改读者管理
export function updateReader(data) {
  return request({
    url: '/system/reader',
    method: 'put',
    data: data
  })
}

// 挂失补办（生成新证号）
export function reissueCard(readerId) {
  return request({ url: '/system/reader/reissue/' + readerId, method: 'put' })
}

// 重置密码：向成员登记邮箱发送重置验证码
export function resetPwdInvite(readerId) {
  return request({ url: '/system/reader/reset-pwd-invite/' + readerId, method: 'post' })
}

// 管理员直接设置密码（代客设密）
export function setPassword(data) {
  return request({ url: '/system/reader/set-password', method: 'put', data: data })
}

// 成员登录日志（后台审计查询）
export function listLoginLog(query) {
  return request({
    url: '/system/reader/loginLog',
    method: 'get',
    params: query
  })
}

export function delReader(readerId) {
  return request({
    url: '/system/reader/' + readerId,
    method: 'delete'
  })
}

// 查询已删除读者列表（后台回收站视图）
export function listDeletedReader(query) {
  return request({
    url: '/system/reader/deletedList',
    method: 'get',
    params: query
  })
}

// 恢复已删除读者
export function restoreReader(readerIds) {
  return request({
    url: '/system/reader/restore/' + readerIds,
    method: 'put'
  })
}

// 永久删除读者
export function purgeReader(readerIds) {
  return request({
    url: '/system/reader/purge/' + readerIds,
    method: 'delete'
  })
}
