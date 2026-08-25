import request from '@/utils/request'

// 首页数据看板：业务聚合统计
export function getDashboard() {
  return request({ url: '/system/dashboard/stats', method: 'get' })
}

// 最近编辑记录（文章/区块各 5 条）
export function getRecentEdits() {
  return request({ url: '/system/dashboard/recentEdits', method: 'get' })
}
