<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="读者姓名" prop="readerName">
        <el-input v-model="queryParams.readerName" placeholder="请输入读者姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="图书名称" prop="bookName">
        <el-input v-model="queryParams.bookName" placeholder="请输入图书名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in statusOptions" :key="dict.dictValue" :label="dict.dictLabel" :value="dict.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:borrow:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reserveList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="reserveId" width="80" />
      <el-table-column label="读者" align="center" prop="readerName" width="120" />
      <el-table-column label="借书证号" align="center" prop="cardNo" width="120" />
      <el-table-column label="图书" align="center" prop="bookName" min-width="140" />
      <el-table-column label="预约时间" align="center" prop="reserveDate" width="160" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="warning">预约中</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="success">可借</el-tag>
          <el-tag v-else-if="scope.row.status === '2'" type="info">已完成</el-tag>
          <el-tag v-else type="info">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:borrow:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listReserve, delReserve } from "@/api/system/reserve"

export default {
  name: "Reserve",
  data() {
    return {
      loading: true,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      reserveList: [],
      statusOptions: [
        { dictValue: '0', dictLabel: '预约中' },
        { dictValue: '1', dictLabel: '可借' },
        { dictValue: '2', dictLabel: '已完成' },
        { dictValue: '3', dictLabel: '已取消' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, readerName: null, bookName: null, status: null }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listReserve(this.queryParams).then(response => {
        this.reserveList = response.rows
        this.total = response.total
      }).catch(() => {
        this.$modal.msgError("预约列表加载失败，请检查网络后重试")
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.reserveId)
      this.multiple = !selection.length
    },
    handleDelete(row) {
      const reserveIds = row.reserveId || this.ids
      this.$modal.confirm('确认删除该预约记录吗？').then(() => {
        return delReserve(reserveIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
