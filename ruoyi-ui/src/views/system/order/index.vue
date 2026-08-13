<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="读者" prop="readerName">
        <el-input v-model="queryParams.readerName" placeholder="请输入读者姓名" clearable @keyup.enter.native="handleQuery" />
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

    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="订单ID" align="center" prop="orderId" width="70" />
      <el-table-column label="订单号" align="center" prop="orderNo" width="170" />
      <el-table-column label="读者" align="center" prop="readerName" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.readerName }}</span>
          <div style="font-size:12px;color:#999">{{ scope.row.cardNo }}</div>
        </template>
      </el-table-column>
      <el-table-column label="图书" align="center" prop="bookName" min-width="160" />
      <el-table-column label="数量" align="center" prop="quantity" width="70" />
      <el-table-column label="金额" align="center" width="100">
        <template slot-scope="scope">
          <span style="color:#e64340;font-weight:bold">¥{{ scope.row.totalPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="warning">待处理</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="success">已完成</el-tag>
          <el-tag v-else type="info">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="success" @click="handleStatus(scope.row, '1')" v-hasPermi="['system:order:edit']">完成</el-button>
          <el-button size="mini" type="info" @click="handleStatus(scope.row, '2')" v-hasPermi="['system:order:edit']">取消</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:order:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listOrder, updateOrder, delOrder } from "@/api/system/order"

export default {
  name: "Order",
  data() {
    return {
      loading: true,
      showSearch: true,
      orderList: [],
      total: 0,
      statusOptions: [
        { dictValue: '0', dictLabel: '待处理' },
        { dictValue: '1', dictLabel: '已完成' },
        { dictValue: '2', dictLabel: '已取消' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, orderNo: null, readerName: null, status: null }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listOrder(this.queryParams).then(response => {
        this.orderList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleStatus(row, status) {
      const text = status === '1' ? '确认该订单已完成？' : '确认取消该订单？'
      this.$modal.confirm(text).then(() => {
        return updateOrder({ orderId: row.orderId, status: status })
      }).then(() => {
        this.$modal.msgSuccess("操作成功")
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该订单吗？').then(() => {
        return delOrder(row.orderId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
