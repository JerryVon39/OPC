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

// ===== 区块管理（cms_block）=====

// 查询区块列表
export function listBlock(query) {
  return request({
    url: '/system/cmsBlock/list',
    method: 'get',
    params: query
  })
}

// 查询区块详情
export function getBlock(blockId) {
  return request({
    url: '/system/cmsBlock/' + blockId,
    method: 'get'
  })
}

// 新增区块
export function addBlock(data) {
  return request({
    url: '/system/cmsBlock',
    method: 'post',
    data: data
  })
}

// 修改区块（自动写历史 + version+1）
export function updateBlock(data) {
  return request({
    url: '/system/cmsBlock',
    method: 'put',
    data: data
  })
}

// 删除区块
export function delBlock(blockIds) {
  return request({
    url: '/system/cmsBlock/' + blockIds,
    method: 'delete'
  })
}

// 区块历史列表
export function listBlockHistory(blockId) {
  return request({
    url: '/system/cmsBlock/history/' + blockId,
    method: 'get'
  })
}

// 回滚到指定历史版本
export function rollbackBlock(blockId, version) {
  return request({
    url: '/system/cmsBlock/rollback/' + blockId + '/' + version,
    method: 'put'
  })
}

// 文章历史列表（version 倒序，最多 20 版）
export function listArticleHistory(articleId) {
  return request({
    url: '/system/cms/history/' + articleId,
    method: 'get'
  })
}

// 回滚文章到指定版本
export function rollbackArticle(articleId, version) {
  return request({
    url: '/system/cms/rollback/' + articleId + '/' + version,
    method: 'put'
  })
}

// 内容区块上下移（相邻 sort 交换）
export function moveBlock(blockId, dir) {
  return request({
    url: '/system/cmsBlock/move/' + blockId + '/' + dir,
    method: 'put'
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

// ===== 首页模块（页面搭建 cms_page_section）=====

// 查询模块列表
export function listSection(query) {
  return request({ url: '/system/cmsSection/list', method: 'get', params: query })
}

// 查询模块详情
export function getSection(sectionId) {
  return request({ url: '/system/cmsSection/' + sectionId, method: 'get' })
}

// 新增模块
export function addSection(data) {
  return request({ url: '/system/cmsSection', method: 'post', data: data })
}

// 修改模块
export function updateSection(data) {
  return request({ url: '/system/cmsSection', method: 'put', data: data })
}

// 删除模块
export function delSection(sectionIds) {
  return request({ url: '/system/cmsSection/' + sectionIds, method: 'delete' })
}

// 上移/下移
export function moveSection(sectionId, dir) {
  return request({ url: '/system/cmsSection/move/' + sectionId + '/' + dir, method: 'put' })
}

// 2：一键复制文章（克隆为草稿）
export function copyArticle(articleId) {
  return request({ url: '/system/cms/copy/' + articleId, method: 'post' })
}

// 6：批量移动栏目
export function batchMoveCategory(data) {
  return request({ url: '/system/cms/batchMoveCategory', method: 'put', data: data })
}

// 5：浏览量报表
export function getCmsStats() {
  return request({ url: '/system/cms/stats', method: 'get' })
}

// 2：一键复制区块
export function copyBlock(blockId) {
  return request({ url: '/system/cmsBlock/copy/' + blockId, method: 'post' })
}

// B：栏目文章数聚合（栏目管理统计列）
export function getCategoryCounts() {
  return request({ url: '/system/cmsCategory/articleCounts', method: 'get' })
}

// 75：自定义前台页面
export function listCmsPage() {
  return request({ url: '/system/cmsPage/list', method: 'get' })
}
export function addCmsPage(data) {
  return request({ url: '/system/cmsPage', method: 'post', data: data })
}
export function updateCmsPage(data) {
  return request({ url: '/system/cmsPage', method: 'put', data: data })
}
export function delCmsPage(pageId) {
  return request({ url: '/system/cmsPage/' + pageId, method: 'delete' })
}
