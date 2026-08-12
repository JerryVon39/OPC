<template>
  <div class="app-container home">
    <!-- 欢迎横幅 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-title">📚 欢迎使用图书管理系统</div>
      <div class="welcome-sub">{{ userName }}，今天是 {{ today }}，祝您工作愉快！</div>
    </el-card>

    <!-- 数据统计 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8" v-for="item in statCards" :key="item.label">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-num" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 + 系统简介 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>⚡ 快捷入口</span>
          </div>
          <div class="quick-links">
            <el-button type="primary" size="medium" icon="el-icon-notebook-2" @click="goBook">图书信息管理</el-button>
            <el-button type="success" size="medium" icon="el-icon-plus" @click="goBookAdd">新增图书</el-button>
            <el-button type="warning" size="medium" icon="el-icon-collection-tag" @click="goDict">字典管理</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>📖 图书类型分布</span>
          </div>
          <div class="type-list">
            <el-tag v-for="t in bookTypes" :key="t.dictValue" :type="tagType(t.listClass)" size="medium" class="type-tag">
              {{ t.dictLabel }}
            </el-tag>
            <el-empty v-if="!bookTypes.length" description="暂无图书分类" :image-size="60"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统简介 -->
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span>ℹ️ 系统简介</span>
      </div>
      <div class="intro-text">
        <p>图书管理系统是一个基于若依（RuoYi-Vue）框架搭建的图书信息管理平台，提供图书信息的增删改查、分类管理、库存跟踪等核心功能。</p>
        <p>系统支持：图书信息维护、图书分类字典、状态管理（在架/下架）、Excel 导入导出、操作日志审计与细粒度权限控制。</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { listBook } from '@/api/system/book'
import { listBorrow } from '@/api/system/borrow'
import { getDicts } from '@/api/system/dict/data'

export default {
  name: 'Index',
  data() {
    return {
      // 统计卡片
      statCards: [
        { label: '图书总数', value: 0, color: '#409EFF' },
        { label: '在架图书', value: 0, color: '#67C23A' },
        { label: '借出中', value: 0, color: '#E6A23C' },
        { label: '已下架', value: 0, color: '#F56C6C' }
      ],
      bookTypes: [],
      today: '',
      userName: ''
    }
  },
  created() {
    this.userName = this.$store.state.user.name || '管理员'
    this.today = this.getToday()
    this.loadStats()
    this.loadDicts()
  },
  methods: {
    /** 当前日期 */
    getToday() {
      const d = new Date()
      const week = ['日', '一', '二', '三', '四', '五', '六']
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week[d.getDay()]}`
    },
    /** 统计图书数量（按状态） */
    loadStats() {
      listBook({ pageNum: 1, pageSize: 1 }).then(res => {
        this.statCards[0].value = res.total || 0
      })
      listBook({ pageNum: 1, pageSize: 1, status: '0' }).then(res => {
        this.statCards[1].value = res.total || 0
      })
      listBook({ pageNum: 1, pageSize: 1, status: '1' }).then(res => {
        this.statCards[3].value = res.total || 0
      })
      listBorrow({ pageNum: 1, pageSize: 1, status: '0' }).then(res => {
        this.statCards[2].value = res.total || 0
      })
    },
    /** 加载图书分类字典 */
    loadDicts() {
      getDicts('book_type').then(res => {
        this.bookTypes = res.data || []
      })
    },
    tagType(cls) {
      const map = { primary: '', success: 'success', info: 'info', warning: 'warning', danger: 'danger' }
      return map[cls] || ''
    },
    goBook() {
      this.$router.push('/system/book')
    },
    goBookAdd() {
      this.$router.push('/system/book')
      setTimeout(() => {
        // 跳转后由列表页自行打开新增，这里延迟触发一次点击提示
        this.$modal.msgSuccess('请点击列表页的【新增】按钮录入图书')
      }, 500)
    },
    goDict() {
      this.$router.push('/system/dict')
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
}
.stat-num {
  font-size: 32px;
  font-weight: bold;
}
.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
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
.type-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.type-tag {
  font-size: 14px;
}
.intro-text {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}
.intro-text p {
  margin: 0 0 8px;
}
</style>
