<template>
  <div class="app-container">
    <!-- 回收站概览 -->
    <el-alert
      class="recycle-tip"
      :type="bookCount > 0 ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="bookCount > 0 ? '回收站暂存 ' + bookCount + ' 项服务，可还原到服务列表；彻底删除后无法恢复。' : '回收站为空，被删除的服务会保留在此，可随时还原或彻底删除。'"
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
import { listDeletedBook, restoreBook, purgeBook } from "@/api/system/book"
import { getDicts } from "@/api/system/dict/data"

export default {
  name: "RecycleBook",
  data() {
    return {
      // 服务类型字典
      bookTypeOptions: [],
      // 遮罩层
      loading: true,
      // 选中服务ID数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 回收站内服务数量（顶部概览）
      bookCount: 0,
      // 回收站服务数据
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
    /** 查询回收站服务数量（取第 1 页 total 即可） */
    getCount() {
      listDeletedBook({ pageNum: 1, pageSize: 1 }).then(res => {
        this.bookCount = res.total || 0
      })
    },
    /** 查询回收站服务列表（两态软删除：del_flag='2'） */
    getList() {
      this.loading = true
      listDeletedBook(this.queryParams).then(response => {
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
    // 多选框选中数据（服务ID）
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.bookId)
      this.multiple = !selection.length
    },
    /** 还原：del_flag 置 '0'，重新对前台/列表可见 */
    handleRestore(row) {
      const ids = row.bookId != null ? [row.bookId] : this.ids
      this.$modal.confirm('确认还原选中的 ' + ids.length + ' 项服务？还原后出现在服务列表中。').then(() => {
        return restoreBook(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('还原成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    },
    /** 彻底删除：物理删除，不可恢复 */
    handlePurge(row) {
      const ids = row.bookId != null ? [row.bookId] : this.ids
      this.$modal.confirm('彻底删除后无法恢复，确认删除选中的 ' + ids.length + ' 项回收站服务？').then(() => {
        return purgeBook(ids.join(','))
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