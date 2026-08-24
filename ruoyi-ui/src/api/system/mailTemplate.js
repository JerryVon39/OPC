import request from '@/utils/request'

// 查询邮件模板列表
export function listTemplate(query) {
  return request({
    url: '/system/mail/template/list',
    method: 'get',
    params: query
  })
}

// 查询邮件模板详情
export function getTemplate(code) {
  return request({
    url: '/system/mail/template/' + code,
    method: 'get'
  })
}

// 保存邮件模板（新增或更新）
export function updateTemplate(data) {
  return request({
    url: '/system/mail/template',
    method: 'put',
    data: data
  })
}
