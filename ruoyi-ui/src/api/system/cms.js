import request from '@/utils/request'

// 查询文章列表
export function listArticle(query) {
  return request({
    url: '/system/cms/list',
    method: 'get',
    params: query
  })
}

// 查询文章详细
export function getArticle(articleId) {
  return request({
    url: '/system/cms/' + articleId,
    method: 'get'
  })
}

// 新增文章
export function addArticle(data) {
  return request({
    url: '/system/cms',
    method: 'post',
    data: data
  })
}

// 修改文章
export function updateArticle(data) {
  return request({
    url: '/system/cms',
    method: 'put',
    data: data
  })
}

// 删除文章（软删除：移入回收站）
export function delArticle(articleId) {
  return request({
    url: '/system/cms/' + articleId,
    method: 'delete'
  })
}

// 查询回收站文章列表
export function listDeletedArticle(query) {
  return request({
    url: '/system/cms/deletedList',
    method: 'get',
    params: query
  })
}

// 恢复回收站文章
export function restoreArticle(articleIds) {
  return request({
    url: '/system/cms/restore/' + articleIds,
    method: 'put'
  })
}

// 永久删除回收站文章
export function purgeArticle(articleIds) {
  return request({
    url: '/system/cms/purge/' + articleIds,
    method: 'delete'
  })
}

// 批量置顶/取消置顶
export function batchTop(articleIds, isTop) {
  return request({
    url: '/system/cms/batchTop',
    method: 'put',
    params: { articleIds: articleIds.join(','), isTop: isTop }
  })
}

// 批量状态切换（0已发布 1草稿 2已下线）
export function batchStatus(articleIds, status) {
  return request({
    url: '/system/cms/batchStatus',
    method: 'put',
    params: { articleIds: articleIds.join(','), status: status }
  })
}

// 批量排序（逐条更新 sort）
export function batchSort(list) {
  return request({
    url: '/system/cms/batchSort',
    method: 'put',
    data: list
  })
}

// 状态切换（0已发布 1草稿 2已下线）
export function changeArticleStatus(articleId, status) {
  return request({
    url: '/system/cms/changeStatus',
    method: 'put',
    params: { articleId: articleId, status: status }
  })
}

// 发布文章（草稿/已下线 → 已发布）
export function publishArticle(articleId) {
  return request({
    url: '/system/cms/publish',
    method: 'put',
    params: { articleId: articleId }
  })
}

// 查询栏目列表
export function listCategory(query) {
  return request({
    url: '/system/cmsCategory/list',
    method: 'get',
    params: query
  })
}

// 查询栏目详细
export function getCategory(categoryId) {
  return request({
    url: '/system/cmsCategory/' + categoryId,
    method: 'get'
  })
}

// 新增栏目
export function addCategory(data) {
  return request({
    url: '/system/cmsCategory',
    method: 'post',
    data: data
  })
}

// 修改栏目
export function updateCategory(data) {
  return request({
    url: '/system/cmsCategory',
    method: 'put',
    data: data
  })
}

// 删除栏目
export function delCategory(categoryId) {
  return request({
    url: '/system/cmsCategory/' + categoryId,
    method: 'delete'
  })
}
