<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目/组织名称" prop="bookName">
        <el-input
          v-model="queryParams.bookName"
          placeholder="请输入项目/组织名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待审核" value="0" />
          <el-option label="已通过" value="1" />
          <el-option label="已婉拒" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:purchase:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="purchaseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="申请ID" align="center" prop="reqId" width="80" />
      <el-table-column label="项目/组织名称" align="center" prop="bookName" min-width="160" show-overflow-tooltip />
      <el-table-column label="联系人" align="center" prop="author" width="120" show-overflow-tooltip />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请说明" align="center" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="提交时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:purchase:edit']">审核</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:purchase:remove']">删除</el-button>
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

    <!-- 处理入驻申请对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" label-width="80px">
        <el-form-item label="项目/组织名称">
          <el-input v-model="form.bookName" :disabled="true" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.author" :disabled="true" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">已通过（已入驻）</el-radio>
            <el-radio label="2">已婉拒</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPurchase, getPurchase, updatePurchase, delPurchase } from "@/api/system/purchase"

export default {
  name: "Purchase",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 入驻申请表格数据
      purchaseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        bookName: null,
        status: null
      },
      // 表单参数
      form: {}
    }
  },
  created() {
    this.getList()
  },
  methods: {
    statusLabel(v) {
      return { '0': '待审核', '1': '已通过', '2': '已婉拒' }[v] || '未知'
    },
    statusType(v) {
      return { '0': 'warning', '1': 'success', '2': 'info' }[v] || 'info'
    },
    /** 查询入驻申请列表 */
    getList() {
      this.loading = true
      listPurchase(this.queryParams).then(response => {
        this.purchaseList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { // 错误已由 request.js 拦截器统一弹出，这里吞掉避免重复 toast
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        reqId: null,
        bookName: null,
        author: null,
        remark: null,
        status: '1'
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.reqId)
      this.multiple = !selection.length
    },
    /** 处理入驻申请（只改状态与处理人） */
    handleUpdate(row) {
      this.reset()
      const reqId = row.reqId || this.ids
      getPurchase(reqId).then(response => {
        this.form = response.data
        this.form.status = this.form.status === '1' || this.form.status === '2' ? this.form.status : '1'
        this.open = true
        this.title = "处理入驻申请"
      })
    },
    submitForm() {
      updatePurchase(this.form).then(response => {
        this.$modal.msgSuccess("审核成功")
        this.open = false
        this.getList()
      })
    },
    handleDelete(row) {
      const reqIds = row.reqId || this.ids
      this.$modal.confirm('是否确认删除入驻申请编号为"' + reqIds + '"的数据项？').then(function() {
        return delPurchase(reqIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>