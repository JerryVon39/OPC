<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon title="内容统计 = 文章浏览量报表：Top20 + 栏目分布 + 近 30 天趋势。趋势数据自本功能上线起按日累计。" />

    <el-row :gutter="16">
      <!-- 左：Top20 -->
      <el-col :span="12">
        <el-card shadow="never" class="stat-card">
          <div slot="header">🔥 浏览量 Top 20</div>
          <el-table :data="topArticles" size="mini" :show-header="true" max-height="520">
            <el-table-column type="index" label="#" width="45" align="center" />
            <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
            <el-table-column label="栏目" prop="categoryName" width="100" show-overflow-tooltip />
            <el-table-column label="浏览量" prop="views" width="80" align="center">
              <template slot-scope="scope"><span style="font-weight:600;color:#409EFF">{{ scope.row.views }}</span></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：栏目分布 -->
      <el-col :span="12">
        <el-card shadow="never" class="stat-card">
          <div slot="header">📊 栏目浏览量分布</div>
          <el-table :data="byCategory" size="mini" max-height="520">
            <el-table-column label="栏目" prop="categoryName" min-width="160" show-overflow-tooltip />
            <el-table-column label="文章数" prop="articleCount" width="90" align="center" />
            <el-table-column label="总浏览量" prop="totalViews" width="110" align="center">
              <template slot-scope="scope"><span style="font-weight:600;color:#E6A23C">{{ scope.row.totalViews }}</span></template>
            </el-table-column>
            <el-table-column label="占比" width="150" align="center">
              <template slot-scope="scope">
                <el-progress :percentage="percentOf(scope.row.totalViews)" :stroke-width="10" :show-text="false" style="width:90%;display:inline-block" />
                <span style="font-size:12px;color:#999;margin-left:4px">{{ percentOf(scope.row.totalViews) }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近 30 天趋势 -->
    <el-card shadow="never" class="stat-card" style="margin-top:16px">
      <div slot="header">📈 近 30 天浏览量趋势</div>
      <div ref="trendChart" style="height:300px"></div>
      <div v-if="!trend.length" style="color:#999;font-size:13px;padding:20px 0;text-align:center">暂无访问数据——前台文章被访问后按日累计，次日可见趋势。</div>
    </el-card>
  </div>
</template>

<script>
import { getCmsStats } from "@/api/system/cms"

export default {
  name: "CmsReport",
  data() {
    return {
      topArticles: [],
      byCategory: [],
      trend: [],
      totalViews: 0
    }
  },
  created() {
    this.loadStats()
  },
  methods: {
    loadStats() {
      getCmsStats().then(res => {
        const d = res.data || {}
        this.topArticles = d.topArticles || []
        this.byCategory = d.byCategory || []
        this.trend = d.trend || []
        this.totalViews = this.byCategory.reduce((s, c) => s + Number(c.totalViews || 0), 0)
        this.$nextTick(() => this.drawTrend())
      }).catch(() => {})
    },
    percentOf(v) {
      if (!this.totalViews) return 0
      return Math.round(Number(v || 0) / this.totalViews * 100)
    },
    /** 简单折线（SVG 手绘，避免引图表库） */
    drawTrend() {
      const el = this.$refs.trendChart
      if (!el || !this.trend.length) return
      const W = el.clientWidth, H = 300
      const pad = { l: 40, r: 16, t: 12, b: 28 }
      const max = Math.max(...this.trend.map(t => Number(t.viewCount)), 1)
      const x = i => pad.l + (W - pad.l - pad.r) * (this.trend.length === 1 ? 0 : i / (this.trend.length - 1))
      const y = v => H - pad.b - (H - pad.t - pad.b) * (v / max)
      const pts = this.trend.map((t, i) => x(i) + ',' + y(Number(t.viewCount)))
      let svg = '<svg width="100%" height="' + H + '" xmlns="http://www.w3.org/2000/svg">'
      // 网格线（4 档）
      for (let g = 0; g <= 4; g++) {
        const gy = pad.t + (H - pad.t - pad.b) * g / 4
        svg += '<line x1="' + pad.l + '" y1="' + gy + '" x2="' + (W - pad.r) + '" y2="' + gy + '" stroke="#eee" stroke-width="1"/>'
        svg += '<text x="6" y="' + (gy + 4) + '" fill="#999" font-size="11">' + Math.round(max * (4 - g) / 4) + '</text>'
      }
      // 折线 + 面积
      svg += '<polygon points="' + pad.l + ',' + (H - pad.b) + ' ' + pts.join(' ') + ' ' + x(this.trend.length - 1) + ',' + (H - pad.b) + '" fill="rgba(64,158,255,.12)" stroke="none"/>'
      svg += '<polyline points="' + pts.join(' ') + '" fill="none" stroke="#409EFF" stroke-width="2" stroke-linejoin="round"/>'
      // 数据点 + 日期标签（最多标 10 个）
      const step = Math.ceil(this.trend.length / 10)
      this.trend.forEach((t, i) => {
        if (i % step === 0 || i === this.trend.length - 1) {
          svg += '<circle cx="' + x(i) + '" cy="' + y(Number(t.viewCount)) + '" r="3" fill="#409EFF"/>'
          const label = String(t.viewDate || '').slice(5)
          svg += '<text x="' + x(i) + '" y="' + (H - 8) + '" fill="#909399" font-size="10" text-anchor="middle">' + label + '</text>'
        }
      })
      svg += '</svg>'
      el.innerHTML = svg
    }
  }
}
</script>

<style scoped>
.stat-card { border-radius: 8px; }
</style>
