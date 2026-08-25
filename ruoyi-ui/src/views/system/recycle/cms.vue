<template>
  <div class="app-container">
    <!-- 回收站概览 -->
    <el-alert
      class="recycle-tip"
      :type="articleCount > 0 ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="articleCount > 0 ? '回收站暂存 ' + articleCount + ' 篇文章，可还原到文章列表；彻底删除后无法恢复。' : '回收站为空，被删除的文章会保留在此，可随时还原或彻底删除。'"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-refresh-left" size="mini" :disabled="multiple" @click="handleRestore">还原</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete-solid" size="mini" :disabled="multiple" @click="handlePurge">彻底删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标题" align="left" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="栏目" align="center" prop="categoryName" width="100">
        <template slot-scope="scope">
          {{ scope.row.categoryName || '未分类' }}
        </template>
      </el-table-column>
      <el-table-column label="原状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">已发布</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="warning" size="mini">草稿</el-tag>
          <el-tag v-else type="info" size="mini">已下线</el-tag>
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
import { listDeletedArticle, restoreArticle, purgeArticle } from "@/api/system/cms"

export default {
  name: "RecycleCms",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 选中文章ID数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 总条数
      total: 0,
      // 回收站内文章数量（顶部概览）
      articleCount: 0,
      // 回收站文章数据
      articleList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null
      }
    }
  },
  created() {
    this.getCount()
    this.getList()
  },
  methods: {
    /** 查询回收站文章数量（取第 1 页 total 即可） */
    getCount() {
      listDeletedArticle({ pageNum: 1, pageSize: 1 }).then(res => {
        this.articleCount = res.total || 0
      })
    },
    /** 查询回收站文章列表（两态软删除：del_flag='2'） */
    getList() {
      this.loading = true
      listDeletedArticle(this.queryParams).then(response => {
        this.articleList = response.rows
        this.total = response.total
      }).finally(() => { this.loading = false })
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
    // 多选框选中数据（文章ID）
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.articleId)
      this.multiple = !selection.length
    },
    /** 还原：del_flag 置 '0'，重新对前台/列表可见 */
    handleRestore(row) {
      const ids = row.articleId != null ? [row.articleId] : this.ids
      this.$modal.confirm('确认还原选中的 ' + ids.length + ' 篇文章？还原后出现在文章列表中。').then(() => {
        return restoreArticle(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('还原成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    },
    /** 彻底删除：物理删除，不可恢复 */
    handlePurge(row) {
      const ids = row.articleId != null ? [row.articleId] : this.ids
      this.$modal.confirm('彻底删除后无法恢复，确认删除选中的 ' + ids.length + ' 篇回收站文章？').then(() => {
        return purgeArticle(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
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
