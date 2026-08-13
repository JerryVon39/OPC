import request from '@/utils/request'

// 首页数据看板：业务聚合统计
export function getDashboard() {
  return request({ url: '/system/dashboard/stats', method: 'get' })
}
