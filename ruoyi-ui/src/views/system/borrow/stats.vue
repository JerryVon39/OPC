<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 热门服务 Top10 -->
      <el-col :xs="24" :sm="12">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight:bold">🔥 热门服务 Top10</span>
          </div>
          <el-table :data="topBooks" size="small">
            <el-table-column label="排名" width="60" align="center">
              <template slot-scope="scope">
                <el-tag v-if="scope.$index < 3" type="danger" size="mini">{{ scope.$index + 1 }}</el-tag>
                <span v-else>{{ scope.$index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="服务名称" prop="bookName" />
            <el-table-column label="报名次数" prop="borrowCount" width="100" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.borrowCount > 0 ? 'success' : 'info'">{{ scope.row.borrowCount }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!topBooks.length" description="暂无报名数据" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 成员报名排行 Top10 -->
      <el-col :xs="24" :sm="12">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight:bold">🏆 成员报名排行 Top10</span>
          </div>
          <el-table :data="topReaders" size="small">
            <el-table-column label="排名" width="60" align="center">
              <template slot-scope="scope">
                <el-tag v-if="scope.$index < 3" type="warning" size="mini">{{ scope.$index + 1 }}</el-tag>
                <span v-else>{{ scope.$index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成员" prop="readerName" />
            <el-table-column label="成员编号" prop="cardNo" width="120" />
            <el-table-column label="报名次数" prop="borrowCount" width="100" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.borrowCount > 0 ? 'success' : 'info'">{{ scope.row.borrowCount }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!topReaders.length" description="暂无报名数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { borrowStats, borrowReaderStats } from "@/api/system/borrow"

export default {
  name: "BorrowStats",
  data() {
    return {
      topBooks: [],
      topReaders: []
    }
  },
  created() {
    this.getStats()
  },
  methods: {
    getStats() {
      // 热门服务走匿名接口；成员排行含证号，走权限接口（匿名接口已不下发证号）
      borrowStats().then(res => {
        this.topBooks = (res.data && res.data.topBooks) || []
      })
      borrowReaderStats().then(res => {
        this.topReaders = res.data || []
      })
    }
  }
}
</script>
