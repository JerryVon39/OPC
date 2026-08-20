<template>
  <div class="app-container">
    <!-- 回收站概览 -->
    <el-alert
      class="recycle-tip"
      :type="bookCount > 0 ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="bookCount > 0 ? '回收站暂存 ' + bookCount + ' 本图书，可还原到图书列表；清空后无法恢复。' : '回收站为空，被删除的图书会暂存 30 天，可随时在此还原。'"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-refresh-left" size="mini" :disabled="multiple" @click="handleRestore">还原</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete-solid" size="mini" :disabled="multiple" @click="handlePurge">彻底删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-delete" size="mini" @click="handleClear">清空回收站</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="图书名称" prop="bookName">
        <el-input v-model="queryParams.bookName" placeholder="请输入图书名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="封面" align="center" width="80">
        <template slot-scope="scope">
          <el-image v-if="scope.row.cover" :src="imgUrl(scope.row.cover)" style="width:44px;height:58px" fit="cover" :preview-src-list="[imgUrl(scope.row.cover)]" />
          <span v-else>📕</span>
        </template>
      </el-table-column>
      <el-table-column label="图书名称" align="center" prop="bookName" min-width="140" />
      <el-table-column label="作者" align="center" prop="author" />
      <el-table-column label="图书类型" align="center" prop="bookType" width="90">
        <template slot-scope="scope">
          <dict-tag :options="bookTypeOptions" :value="scope.row.bookType" />
        </template>
      </el-table-column>
      <el-table-column label="价格(元)" align="center" prop="price" width="90" />
      <el-table-column label="库存" align="center" prop="stock" width="70" />
      <el-table-column label="原状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">在架</el-tag>
          <el-tag v-else type="info" size="mini">下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="删除人" align="center" prop="deletedBy" width="100" />
      <el-table-column label="删除时间" align="center" width="165">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.deletedTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="handleRestore(scope.row)">还原</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete-solid" @click="handlePurge(scope.row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listRecycleBook, countRecycleBook, restoreRecycleBook, delRecycleBook, clearRecycleBook } from "@/api/system/recycle"
import { getDicts } from "@/api/system/dict/data"

export default {
  name: "RecycleBook",
  data() {
    return {
      // 图书类型字典
      bookTypeOptions: [],
      // 遮罩层
      loading: true,
      // 选中回收站ID数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 回收站内图书数量（顶部概览）
      bookCount: 0,
      // 回收站图书数据
      bookList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        bookName: null
      }
    }
  },
  created() {
    this.getDicts("book_type").then(response => {
      this.bookTypeOptions = response.data
    })
    this.getCount()
    this.getList()
  },
  methods: {
    /** 封面相对路径转完整地址 */
    imgUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    /** 查询回收站图书数量 */
    getCount() {
      countRecycleBook().then(res => {
        this.bookCount = res.data || 0
      })
    },
    /** 查询回收站图书列表 */
    getList() {
      this.loading = true
      listRecycleBook(this.queryParams).then(response => {
        this.bookList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据（回收站ID）
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recycleId)
      this.multiple = !selection.length
    },
    /** 还原：优先保留原图书ID，被占用则自动分配新ID */
    handleRestore(row) {
      const ids = row.recycleId != null ? [row.recycleId] : this.ids
      this.$modal.confirm('确认还原选中的 ' + ids.length + ' 本图书？还原后出现在图书列表中。').then(() => {
        return restoreRecycleBook(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('还原成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    },
    /** 彻底删除：从回收站移除，不可恢复 */
    handlePurge(row) {
      const ids = row.recycleId != null ? [row.recycleId] : this.ids
      this.$modal.confirm('彻底删除后无法恢复，确认删除选中的 ' + ids.length + ' 本回收站图书？').then(() => {
        return delRecycleBook(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    },
    /** 清空回收站 */
    handleClear() {
      this.$modal.confirm('确认清空整个图书回收站？清空后所有暂存图书将无法恢复！').then(() => {
        return clearRecycleBook()
      }).then(() => {
        this.$modal.msgSuccess('清空成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.recycle-tip {
  margin-bottom: 14px;
}
</style>