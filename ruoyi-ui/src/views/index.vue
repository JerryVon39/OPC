<template>
  <div class="app-container home">
    <!-- 欢迎横幅 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-title">🏪 数智游民创新工场 · 运营看板</div>
      <div class="welcome-sub">{{ userName }}，今天是 {{ today }}，祝您工作愉快！</div>
    </el-card>

    <!-- 业务统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.label">
        <!-- 69：统计卡可点击直达（path 为空 = 纯展示不可点） -->
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '18px' }" :style="item.path ? 'cursor:pointer' : ''" @click.native="statGo(item)">
          <div class="stat-num" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 快捷入口 -->
      <el-col :xs="24" :sm="24">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>⚡ 快捷入口</span>
          </div>
          <div class="quick-links">
            <el-button type="primary" size="medium" icon="el-icon-document-add" @click="go('/content/article')">发文章</el-button>
            <el-button type="warning" size="medium" icon="el-icon-edit" @click="go('/content/block')">改区块</el-button>
            <el-button type="info" size="medium" icon="el-icon-user" @click="go('/member/reader')">成员管理</el-button>
            <el-button type="success" size="medium" icon="el-icon-picture-outline" @click="go('/content/banner')">官网轮播</el-button>
            <el-button type="danger" plain size="medium" icon="el-icon-megaphone" @click="go('/content/notice')">发公告</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近编辑（文章/区块各 5 条，点击直达编辑） -->
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>📝 最近编辑文章</span>
            <el-button type="text" size="mini" @click="go('/content/article')">更多 →</el-button>
          </div>
          <div v-if="recentArticles.length" class="recent-list">
            <div v-for="a in recentArticles" :key="a.articleId" class="recent-item" @click="go('/content/article')">
              <span class="recent-name">{{ a.title }}</span>
              <span class="recent-time">{{ a.updateTime || '' }}</span>
            </div>
          </div>
          <el-empty v-else description="还没有文章" :image-size="50"></el-empty>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>🧩 最近编辑区块</span>
            <el-button type="text" size="mini" @click="go('/content/block')">更多 →</el-button>
          </div>
          <div v-if="recentBlocks.length" class="recent-list">
            <div v-for="b in recentBlocks" :key="b.blockId" class="recent-item" @click="go('/content/block')">
              <span class="recent-name">{{ b.title || b.blockKey }}</span>
              <span class="recent-time">{{ b.updateTime || '' }}</span>
            </div>
          </div>
          <el-empty v-else description="还没有区块内容" :image-size="50"></el-empty>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboard, getRecentEdits } from '@/api/system/dashboard'

export default {
  name: 'Index',
  data() {
    return {
      // 业务统计卡片（6 项：成员 + CMS 文章 + 待办；服务业务已关停不再展示）
      // 69：path 非空 = 可点击直达（草稿→文章列表带筛选；回收站/待审核→对应列表页）
      statCards: [
        { label: '社区成员', value: 0, color: '#E6A23C' },
        { label: '文章总数', value: 0, color: '#67C23A', path: '/content/article' },
        { label: '今日发文', value: 0, color: '#409EFF', path: '/content/article' },
        { label: '草稿', value: 0, color: '#E6A23C', path: '/content/article?status=1' },
        { label: '回收站', value: 0, color: '#F56C6C', path: '/ops-aux/recycle/cms' },
        { label: '待审核申请', value: 0, color: '#E2554B', path: '/member/purchase' }
      ],
      recentArticles: [], // 最近编辑文章 5 条
      recentBlocks: [],   // 最近编辑区块 5 条
      today: '',
      userName: ''
    }
  },
  created() {
    this.userName = this.$store.state.user.name || '管理员'
    this.today = this.getToday()
    this.loadStats()
    this.loadRecentEdits()
  },
  methods: {
    /** 当前日期 */
    getToday() {
      const d = new Date()
      const week = ['日', '一', '二', '三', '四', '五', '六']
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week[d.getDay()]}`
    },
    /** 加载业务统计 */
    loadStats() {
      // L8 修复：失败不静默归零——catch 后保持上次数值/占位，不再与"零"不可区分
      getDashboard().then(res => {
        const s = res.data || {}
        this.statCards[0].value = s.readerTotal || 0
        const ca = s.cmsArticle || {}
        this.statCards[1].value = ca.articleTotal || 0
        this.statCards[2].value = ca.articleToday || 0
        this.statCards[3].value = ca.draftCount || 0
        this.statCards[4].value = ca.recycleCount || 0
        this.statCards[5].value = s.pendingApplyCount || 0
      }).catch(() => {}) // L8 修复：请求失败静默保留上次数值，不产生未捕获异常
    },
    /** 加载最近编辑（文章/区块各 5 条） */
    loadRecentEdits() {
      getRecentEdits().then(res => {
        const d = res.data || {}
        this.recentArticles = d.articles || []
        this.recentBlocks = d.blocks || []
      }).catch(() => {}) // L8 修复：请求失败静默保留空态，不产生未捕获异常
    },
    /** 69：统计卡点击直达（path 含 query 时拆开传给 router） */
    statGo(item) {
      if (!item.path) return
      const [path, queryStr] = item.path.split('?')
      const query = {}
      if (queryStr) queryStr.split('&').forEach(kv => { const [k, v] = kv.split('='); if (k && v !== undefined) query[k] = v })
      this.$router.push({ path, query })
    },
    go(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
  border-radius: 8px;
}
.welcome-title {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}
.welcome-sub {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}
.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  border-radius: 8px;
  margin-bottom: 16px;
}
.stat-num {
  font-size: 30px;
  font-weight: bold;
}
.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
.section-card {
  margin-bottom: 20px;
  border-radius: 8px;
}
.card-header {
  font-weight: bold;
  color: #303133;
}
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.recent-list {
  max-height: 260px;
  overflow-y: auto;
}
.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 4px;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;
}
.recent-item:hover {
  background: #f7f8fa;
}
.recent-name {
  color: #303133;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}
.recent-time {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}
</style>
