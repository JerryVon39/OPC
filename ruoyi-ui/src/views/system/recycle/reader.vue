<template>
  <div class="app-container">
    <!-- 回收站概览 -->
    <el-alert
      class="recycle-tip"
      :type="readerCount > 0 ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="readerCount > 0 ? '回收站暂存 ' + readerCount + ' 位成员，可还原到成员列表；彻底删除后无法恢复。' : '回收站为空，被删除的成员会保留在此，可随时还原或彻底删除。'"
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

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="成员姓名" prop="readerName">
        <el-input v-model="queryParams.readerName" placeholder="请输入成员姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="readerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="成员姓名" align="center" prop="readerName" min-width="100" />
      <el-table-column label="手机号" align="center" prop="phone" width="130" />
      <el-table-column label="成员编号" align="center" prop="cardNo" width="160" />
      <el-table-column label="类型" align="center" prop="readerType" width="90">
        <template slot-scope="scope">
          <dict-tag :options="readerTypeOptions" :value="scope.row.readerType" />
        </template>
      </el-table-column>
      <el-table-column label="性别" align="center" prop="sex" width="70" />
      <el-table-column label="原状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">正常</el-tag>
          <el-tag v-else type="info" size="mini">{{ scope.row.status === '1' ? '停用' : '挂失' }}</el-tag>
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
import { listDeletedReader, restoreReader, purgeReader } from "@/api/system/reader"
import { getDicts } from "@/api/system/dict/data"

export default {
  name: "RecycleReader",
  data() {
    return {
      // 成员类型字典
      readerTypeOptions: [],
      // 遮罩层
      loading: true,
      // 选中成员ID数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 回收站内成员数量（顶部概览）
      readerCount: 0,
      // 回收站成员数据
      readerList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        readerName: null
      }
    }
  },
  created() {
    this.getDicts("reader_type").then(response => {
      this.readerTypeOptions = response.data
    })
    this.getCount()
    this.getList()
  },
  methods: {
    /** 查询回收站成员数量（取第 1 页 total 即可） */
    getCount() {
      listDeletedReader({ pageNum: 1, pageSize: 1 }).then(res => {
        this.readerCount = res.total || 0
      })
    },
    /** 查询回收站成员列表（两态软删除：del_flag='2'） */
    getList() {
      this.loading = true
      listDeletedReader(this.queryParams).then(response => {
        this.readerList = response.rows
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
    // 多选框选中数据（成员ID）
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.readerId)
      this.multiple = !selection.length
    },
    /** 还原：del_flag 置 '0'，重新对前台/列表可见 */
    handleRestore(row) {
      const ids = row.readerId != null ? [row.readerId] : this.ids
      this.$modal.confirm('确认还原选中的 ' + ids.length + ' 位成员？还原后出现在成员列表中。').then(() => {
        return restoreReader(ids.join(','))
      }).then(() => {
        this.$modal.msgSuccess('还原成功')
        this.getList()
        this.getCount()
      }).catch(() => {})
    },
    /** 彻底删除：物理删除，不可恢复 */
    handlePurge(row) {
      const ids = row.readerId != null ? [row.readerId] : this.ids
      this.$modal.confirm('彻底删除后无法恢复，确认删除选中的 ' + ids.length + ' 位回收站成员？').then(() => {
        return purgeReader(ids.join(','))
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