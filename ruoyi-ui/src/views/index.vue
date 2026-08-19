<template>
  <div class="app-container home">
    <!-- 欢迎横幅 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-title">🏪 万事屋 · 数据看板</div>
      <div class="welcome-sub">{{ userName }}，今天是 {{ today }}，祝您工作愉快！</div>
    </el-card>

    <!-- 业务统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" v-for="item in statCards" :key="item.label">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '18px' }">
          <div class="stat-num" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 热门图书 Top5 图表 -->
      <el-col :xs="24" :sm="14">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>🔥 热门图书 Top5（按借阅次数）</span>
          </div>
          <div ref="topChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 快捷入口 + 分类 -->
      <el-col :xs="24" :sm="10">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span>⚡ 快捷入口</span>
          </div>
          <div class="quick-links">
            <el-button type="primary" size="medium" icon="el-icon-notebook-2" @click="go('/business/book-mgmt/book')">图书管理</el-button>
            <el-button type="warning" size="medium" icon="el-icon-reading" @click="go('/business/book-mgmt/borrow')">借阅记录</el-button>
            <el-button type="success" size="medium" icon="el-icon-shopping-cart-full" @click="go('/business/ops/order')">订单管理</el-button>
            <el-button type="info" size="medium" icon="el-icon-user" @click="go('/business/reader-mgmt/reader')">读者管理</el-button>
          </div>
          <div class="card-header sub-header">📖 图书分类</div>
          <div class="type-list">
            <el-tag v-for="t in bookTypes" :key="t.dictValue" size="medium" class="type-tag">{{ t.dictLabel }}</el-tag>
            <el-empty v-if="!bookTypes.length" description="暂无图书分类" :image-size="50"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警（在架书库存 ≤ book.stock.warn 阈值，提醒补货） -->
    <el-card shadow="never" class="section-card warn-card">
      <div slot="header" class="card-header">
        <span>🚨 库存预警</span>
        <el-tag v-if="lowStockBooks.length" type="danger" size="mini">{{ lowStockBooks.length }} 本低于阈值</el-tag>
        <el-tag v-else type="success" size="mini">库存充足</el-tag>
      </div>
      <div v-if="lowStockBooks.length" class="warn-list">
        <div v-for="b in lowStockBooks" :key="b.bookId" class="warn-item" @click="goLowStock">
          <span class="warn-name">《{{ b.bookName }}》</span>
          <span class="warn-stock">仅剩 {{ b.stock }} 本</span>
        </div>
      </div>
      <el-empty v-else description="库存充足，无需补货" :image-size="60"></el-empty>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboard } from '@/api/system/dashboard'
import { getDicts } from '@/api/system/dict/data'

export default {
  name: 'Index',
  data() {
    return {
      // 业务统计卡片（8 项）
      statCards: [
        { label: '图书总数', value: 0, color: '#409EFF' },
        { label: '在架图书', value: 0, color: '#67C23A' },
        { label: '读者总数', value: 0, color: '#E6A23C' },
        { label: '未还借阅', value: 0, color: '#F56C6C' },
        { label: '逾期图书', value: 0, color: '#F56C6C' },
        { label: '今日借出', value: 0, color: '#409EFF' },
        { label: '今日订单', value: 0, color: '#67C23A' },
        { label: '订单总数', value: 0, color: '#E6A23C' },
        { label: '欠费总额', value: 0, color: '#F56C6C' }
      ],
      topBooks: [],
      lowStockBooks: [], // 库存预警：在架书库存 ≤ 阈值（book.stock.warn）
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
  mounted() {
    this.initChart()
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
      getDashboard().then(res => {
        const s = res.data || {}
        this.statCards[0].value = s.bookTotal || 0
        this.statCards[1].value = s.bookOnSale || 0
        this.statCards[2].value = s.readerTotal || 0
        this.statCards[3].value = s.borrowingCount || 0
        this.statCards[4].value = s.overdueCount || 0
        this.statCards[5].value = s.borrowToday || 0
        this.statCards[6].value = s.orderToday || 0
        this.statCards[7].value = s.orderTotal || 0
        this.statCards[8].value = '¥' + (s.fineTotal || 0)
        this.topBooks = (s.topBooks || []).slice(0, 5)
        this.lowStockBooks = s.lowStockBooks || []
        this.initChart()
      })
    },
    /** 跳图书管理页（lowStock=1：列表按 book.stock.warn 阈值过滤低库存书） */
    goLowStock() {
      this.$router.push({ path: '/business/book-mgmt/book', query: { lowStock: 1 } })
    },
    /** 热门图书柱状图 */
    initChart() {
      if (!this.$refs.topChart) return
      const chart = echarts.init(this.$refs.topChart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 20, bottom: 40 },
        xAxis: { type: 'category', data: this.topBooks.map(b => b.bookName || '未知'), axisLabel: { interval: 0, rotate: 25, fontSize: 11 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'bar',
          barWidth: 30,
          itemStyle: { color: '#c9a96a', borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top' },
          data: this.topBooks.map(b => b.borrowCount || 0)
        }]
      })
    },
    /** 加载图书分类字典 */
    loadDicts() {
      getDicts('book_type').then(res => {
        this.bookTypes = res.data || []
      })
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
.sub-header {
  margin-top: 18px;
  margin-bottom: 10px;
}
.chart-box {
  height: 320px;
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
  font-size: 13px;
}
/* 库存预警卡片 */
.warn-card {
  margin-top: 16px;
}
.warn-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.warn-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border: 1px solid #f5d9d1;
  background: #fdf0ec;
  border-radius: 8px;
  cursor: pointer;
  transition: transform .15s, box-shadow .15s;
}
.warn-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(198, 93, 67, 0.15);
}
.warn-name {
  font-size: 14px;
  color: #8a4a38;
  font-weight: bold;
}
.warn-stock {
  font-size: 12px;
  color: #c65d43;
  font-weight: bold;
}
</style>
