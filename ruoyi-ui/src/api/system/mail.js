import request from '@/utils/request'

// 查询邮件 SMTP 配置
export function getConfig() {
  return request({
    url: '/system/mail/config',
    method: 'get'
  })
}

// 保存邮件 SMTP 配置
export function saveConfig(data) {
  return request({
    url: '/system/mail/config',
    method: 'put',
    data: data
  })
}

// 测试发送邮件
export function testSend(to) {
  return request({
    url: '/system/mail/config/test',
    method: 'post',
    params: { to: to }
  })
}
