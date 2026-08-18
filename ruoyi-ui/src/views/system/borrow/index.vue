<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="读者" prop="readerName">
        <el-input v-model="queryParams.readerName" placeholder="请输入读者姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="图书" prop="bookName">
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:borrow:add']">借书</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:borrow:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="borrowList">
      <el-table-column label="借阅ID" align="center" prop="borrowId" width="80" />
      <el-table-column label="读者" align="center" min-width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.readerName || 'ID:' + scope.row.readerId }}</span>
          <div style="font-size:12px;color:#999">{{ scope.row.cardNo }}</div>
        </template>
      </el-table-column>
      <el-table-column label="图书" align="center" prop="bookName" min-width="180">
        <template slot-scope="scope">
          <span>{{ scope.row.bookName || 'ID:' + scope.row.bookId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借出日期" align="center" prop="borrowDate" width="110" />
      <el-table-column label="应还日期" align="center" prop="dueDate" width="110" />
      <el-table-column label="归还日期" align="center" prop="returnDate" width="110" />
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="primary">借出中</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="success">已归还</el-tag>
          <el-tag v-else type="danger">已逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="罚款" align="center" width="110">
        <template slot-scope="scope">
          <template v-if="scope.row.fineAmount && scope.row.fineAmount > 0">
            <span style="color:#c65d43;font-weight:bold">¥{{ scope.row.fineAmount }}</span>
            <el-tag v-if="scope.row.finePaid === '0'" type="danger" size="mini" style="margin-left:4px">未缴</el-tag>
            <el-tag v-else type="success" size="mini" style="margin-left:4px">已缴</el-tag>
          </template>
          <span v-else style="color:#ccc">—</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="scope.row.status === '0'" size="mini" type="primary" @click="handleRenew(scope.row)" v-hasPermi="['system:borrow:edit']">续借</el-button>
          <el-button v-if="scope.row.status === '0' || scope.row.status === '2'" size="mini" type="success" @click="handleReturn(scope.row)" v-hasPermi="['system:borrow:edit']">还书</el-button>
          <el-button v-if="scope.row.fineAmount > 0 && scope.row.finePaid === '0'" size="mini" type="warning" @click="handlePayFine(scope.row)" v-hasPermi="['system:borrow:edit']">收款</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:borrow:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:borrow:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 借书对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="读者" prop="readerId">
          <el-select v-model="form.readerId" placeholder="请选择读者" filterable style="width:100%">
            <el-option v-for="r in readerOptions" :key="r.readerId" :label="r.readerName + '（' + (r.cardNo || '无证号') + '）'" :value="r.readerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="图书" prop="bookId">
          <el-select v-model="form.bookId" placeholder="请选择图书（显示库存）" filterable style="width:100%">
            <el-option v-for="b in bookOptions" :key="b.bookId" :label="b.bookName + '（库存' + (b.stock || 0) + '）'" :value="b.bookId" :disabled="b.status !== '0' || b.stock <= 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="借出日期" prop="borrowDate">
          <el-date-picker clearable v-model="form.borrowDate" type="date" value-format="yyyy-MM-dd" placeholder="默认今天" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listBorrow, addBorrow, updateBorrow, returnBorrow, renewBorrow, payFine, delBorrow } from "@/api/system/borrow"
import { listReader } from "@/api/system/reader"
import { listBook } from "@/api/system/book"

export default {
  name: "Borrow",
  data() {
    return {
      loading: true,
      showSearch: true,
      borrowList: [],
      total: 0,
      readerOptions: [],
      bookOptions: [],
      statusOptions: [
        { dictValue: '0', dictLabel: '借出中' },
        { dictValue: '1', dictLabel: '已归还' },
        { dictValue: '2', dictLabel: '已逾期' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, readerName: null, bookName: null, status: null },
      title: "",
      open: false,
      form: {},
      rules: {
        readerId: [{ required: true, message: "请选择读者", trigger: "change" }],
        bookId: [{ required: true, message: "请选择图书", trigger: "change" }]
      }
    }
  },
  created() {
    // 支持从读者/图书管理跳转并自动筛选
    const q = this.$route.query
    if (q.readerId) this.queryParams.readerId = q.readerId
    if (q.bookId) this.queryParams.bookId = q.bookId
    this.getList()
    this.loadOptions()
  },
  watch: {
    // 从图书/读者管理再次跳转（路径相同、仅 query 变化）时组件会被复用、created 不再触发，
    // 这里监听路由变化同步筛选条件并刷新列表
    '$route'(to) {
      if (to.path === '/business/borrow') {
        const q = to.query || {}
        this.queryParams.readerId = q.readerId || null
        this.queryParams.bookId = q.bookId || null
        this.queryParams.pageNum = 1
        this.getList()
      }
    }
  },
  methods: {
    getList() {
      this.loading = true
      listBorrow(this.queryParams).then(response => {
        this.borrowList = response.rows
        this.total = response.total
      }).catch(() => {
        // 错误提示已由 request.js 拦截器统一弹出，这里只吞掉异常避免重复 toast
      }).finally(() => {
        this.loading = false
      })
    },
    loadOptions() {
      listReader({ pageNum: 1, pageSize: 100 }).then(res => { this.readerOptions = res.rows || [] })
      listBook({ pageNum: 1, pageSize: 100 }).then(res => { this.bookOptions = res.rows || [] })
    },
    handleExport() {
      this.download('system/borrow/export', { ...this.queryParams }, `borrow_${new Date().getTime()}.xlsx`)
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() {
      // 重置时一并清掉跳转带来的读者/图书筛选，避免"看不见的筛选条件"
      this.queryParams.readerId = null
      this.queryParams.bookId = null
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "借书"
    },
    handleUpdate(row) {
      this.reset()
      this.form = { ...row }
      this.open = true
      this.title = "修改借阅记录"
    },
    handleRenew(row) {
      this.$modal.confirm('确认续借《' + (row.bookName || '') + '》吗？应还日期将顺延 30 天').then(() => {
        return renewBorrow(row.borrowId)
      }).then(() => {
        this.$modal.msgSuccess("续借成功，应还日期顺延 30 天")
        this.getList()
      }).catch(() => {})
    },
    handlePayFine(row) {
      this.$modal.confirm('确认已收到《' + (row.bookName || '') + '》的逾期罚款 ¥' + row.fineAmount + ' 吗？').then(() => {
        return payFine(row.borrowId)
      }).then(() => {
        this.$modal.msgSuccess("收款成功，读者欠费已清零")
        this.getList()
      }).catch(() => {})
    },
    handleReturn(row) {
      this.$modal.confirm('确认还书《' + (row.bookName || '') + '》吗？归还后库存自动+1').then(() => {
        return returnBorrow(row.borrowId)
      }).then(() => {
        this.$modal.msgSuccess("还书成功，库存已恢复（逾期将自动结算罚款）")
        this.getList()
      }).catch(() => {})
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        const api = this.form.borrowId ? updateBorrow : addBorrow
        api(this.form).then(res => {
          this.$modal.msgSuccess(this.form.borrowId ? "修改成功" : "借书成功，库存已-1")
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该借阅记录吗？').then(() => {
        return delBorrow(row.borrowId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    reset() {
      this.form = { readerId: null, bookId: null, borrowDate: null, remark: null }
      this.resetForm("form")
    },
    cancel() { this.open = false; this.reset() }
  }
}
</script>
