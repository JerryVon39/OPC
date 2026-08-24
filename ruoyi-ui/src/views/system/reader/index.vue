<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="成员姓名" prop="readerName">
        <el-input
          v-model="queryParams.readerName"
          placeholder="请输入成员姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号码" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="成员编号" prop="cardNo">
        <el-input
          v-model="queryParams.cardNo"
          placeholder="请输入成员编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="出生日期" prop="birthDate">
        <el-date-picker clearable
          v-model="queryParams.birthDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择出生日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:reader:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:reader:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-time"
          size="mini"
          @click="openLoginLog"
          v-hasPermi="['system:reader:list']"
        >登录日志</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:reader:remove']"
        >删除</el-button>
      </el-col>
      <!-- 一期隐藏：Excel 批量导出/导入按钮（二期再启用，代码与接口保留） -->
      <!--
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:reader:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['system:reader:add']"
        >导入</el-button>
      </el-col>
      -->
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="readerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="成员ID" align="center" prop="readerId" />
      <el-table-column label="成员姓名" align="center" prop="readerName" />
      <el-table-column label="手机号码" align="center" prop="phone" />
      <el-table-column label="电子邮箱" align="center" prop="email" min-width="160" />
      <el-table-column label="成员编号" align="center" prop="cardNo" />
      <el-table-column label="成员类型" align="center" prop="readerType" width="80">
        <template slot-scope="scope">
          <dict-tag :options="readerTypeOptions" :value="scope.row.readerType" />
        </template>
      </el-table-column>
      <el-table-column label="性别" align="center" width="70">
        <template slot-scope="scope">
          <span>{{ { '0': '男', '1': '女', '2': '未知' }[scope.row.sex] || '未知' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="出生日期" align="center" prop="birthDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.birthDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">正常</el-tag>
          <el-tag v-else type="danger" size="mini">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-reading" @click="handleBorrow(scope.row)" v-hasPermi="['system:borrow:list']">报名记录</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-refresh"
            @click="handleReissue(scope.row)"
            v-hasPermi="['system:reader:edit']"
          >重发编号</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-key"
            @click="handleResetPwd(scope.row)"
            v-hasPermi="['system:reader:resetPwd']"
          >重置密码</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:reader:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:reader:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 登录日志对话框 -->
    <el-dialog title="成员登录日志" :visible.sync="logOpen" width="860px" append-to-body>
      <el-form :inline="true" size="small" style="margin-bottom:10px">
        <el-form-item label="成员编号">
          <el-input v-model="logQuery.cardNo" placeholder="证号/手机号/邮箱" clearable style="width:200px" @keyup.enter.native="loadLog" />
        </el-form-item>
        <el-form-item label="事件">
          <el-select v-model="logQuery.event" placeholder="全部" clearable style="width:140px">
            <el-option label="登录" value="login" />
            <el-option label="登录失败" value="login_fail" />
            <el-option label="登出" value="logout" />
            <el-option label="改密" value="change_pwd" />
            <el-option label="重置密码" value="reset_pwd" />
            <el-option label="改邮箱" value="change_email" />
            <el-option label="注册" value="register" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="logQuery.result" placeholder="全部" clearable style="width:100px">
            <el-option label="成功" value="1" />
            <el-option label="失败" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="loadLog">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="logLoading" :data="logList" height="420">
        <el-table-column label="时间" align="center" prop="createTime" width="170" />
        <el-table-column label="成员编号" align="center" prop="cardNo" width="130" />
        <el-table-column label="事件" align="center" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.event === 'login'" type="success" size="mini">登录</el-tag>
            <el-tag v-else-if="scope.row.event === 'login_fail'" type="danger" size="mini">登录失败</el-tag>
            <el-tag v-else-if="scope.row.event === 'logout'" type="info" size="mini">登出</el-tag>
            <el-tag v-else-if="scope.row.event === 'change_pwd'" type="warning" size="mini">改密</el-tag>
            <el-tag v-else-if="scope.row.event === 'reset_pwd'" type="warning" size="mini">重置密码</el-tag>
            <el-tag v-else-if="scope.row.event === 'change_email'" type="warning" size="mini">改邮箱</el-tag>
            <el-tag v-else size="mini">{{ scope.row.event }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" align="center" width="80">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.result === '1'" type="success" size="mini">成功</el-tag>
            <el-tag v-else type="danger" size="mini">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP" align="center" prop="ip" width="140" />
        <el-table-column label="备注" align="center" prop="msg" :show-overflow-tooltip="true" />
      </el-table>
      <pagination v-show="logTotal > 0" :total="logTotal" :page.sync="logQuery.pageNum" :limit.sync="logQuery.pageSize" @pagination="loadLog" />
    </el-dialog>

    <!-- 重置密码对话框：发邀请邮件 / 直接设密 -->
    <el-dialog title="重置成员密码" :visible.sync="pwdOpen" width="480px" append-to-body>
      <div style="margin-bottom:14px">成员：<b>{{ pwdRow.readerName }}</b>（{{ pwdRow.cardNo }}，邮箱：{{ pwdRow.email || '未登记' }}）</div>
      <el-radio-group v-model="pwdMode">
        <el-radio label="invite">发送重置邀请邮件（成员自行在「忘记密码」处设置）</el-radio>
        <el-radio label="direct" style="margin-top:8px">直接设置新密码（管理员代设）</el-radio>
      </el-radio-group>
      <el-input v-if="pwdMode === 'direct'" v-model="pwdNew" type="password" show-password
        placeholder="新密码（≥10 位，含大小写/数字/符号至少 3 类）" style="margin-top:12px" />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="pwdSaving" @click="submitResetPwd">确认</el-button>
        <el-button @click="pwdOpen = false">取 消</el-button>
      </div>
    </el-dialog>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 批量导入对话框 -->
    <el-dialog title="批量导入成员" :visible.sync="importOpen" width="560px" append-to-body>
      <div class="import-tip">
        <p>① 先<a class="import-link" @click="downloadTemplate">下载导入模板</a>，按表头填写数据（姓名/手机号/电子邮箱必填）</p>
        <p>② 成员编号留空将自动生成；编号已存在、类型不在字典内、邮箱缺失/格式错的行会跳过并提示</p>
      </div>
      <el-upload
        :action="importUrl"
        :headers="uploadHeaders"
        accept=".xlsx"
        :show-file-list="false"
        :on-success="handleImportSuccess"
        :on-error="handleImportError"
      >
        <el-button type="primary" icon="el-icon-upload2">选择 Excel 文件上传</el-button>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-alert
          :type="importResult.fail ? 'warning' : 'success'"
          :closable="false"
          show-icon
          :title="'导入完成：成功 ' + importResult.success + ' 条' + (importResult.fail ? '，失败 ' + importResult.fail + ' 条' : '')"
        />
        <ul v-if="importResult.errors && importResult.errors.length" class="import-errors">
          <li v-for="(e, i) in importResult.errors" :key="i">{{ e }}</li>
        </ul>
      </div>
    </el-dialog>

    <!-- 添加或修改成员管理对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="成员姓名" prop="readerName">
              <el-input v-model="form.readerName" placeholder="请输入成员姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="用于接收报名/候补邮件通知" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成员编号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="留空则自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成员类型" prop="readerType">
              <el-select v-model="form.readerType" placeholder="请选择成员类型" style="width:100%">
                <el-option
                  v-for="dict in readerTypeOptions"
                  :key="dict.dictValue"
                  :label="dict.dictLabel"
                  :value="dict.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="form.sex">
                <el-radio label="0">男</el-radio>
                <el-radio label="1">女</el-radio>
                <el-radio label="2">未知</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker clearable
                v-model="form.birthDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出生日期"
                style="width:100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listReader, getReader, delReader, addReader, updateReader, reissueCard, resetPwdInvite, setPassword, listLoginLog } from "@/api/system/reader"
import { getDicts } from "@/api/system/dict/data"
import { getToken } from "@/utils/auth"

export default {
  name: "Reader",
  data() {
    return {
      // 成员类型字典
      readerTypeOptions: [],
      // 批量导入弹窗
      importOpen: false,
      importResult: null,
      importUrl: process.env.VUE_APP_BASE_API + "/system/reader/importData",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 成员管理表格数据
      readerList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        readerName: null,
        phone: null,
        cardNo: null,
        readerType: null,
        sex: null,
        birthDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        readerName: [
          { required: true, message: "成员姓名不能为空", trigger: "blur" }
        ],
        phone: [
          { required: true, message: "手机号码不能为空", trigger: "blur" },
          { pattern: /^\d{11}$/, message: "需 11 位数字", trigger: "blur" }
        ],
        email: [
          { required: true, message: "电子邮箱不能为空（用于邮件通知）", trigger: "blur" },
          { pattern: /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/, message: "邮箱格式不正确", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getDicts("reader_type").then(response => {
      this.readerTypeOptions = response.data;
    });
    this.getList()
  },
  methods: {
    /** 打开登录日志 */
    openLoginLog() {
      this.logOpen = true
      this.logQuery = { pageNum: 1, pageSize: 10, cardNo: '', event: '', result: '' }
      this.loadLog()
    },
    loadLog() {
      this.logLoading = true
      listLoginLog(this.logQuery).then(res => {
        this.logList = res.rows || []
        this.logTotal = res.total || 0
        this.logLoading = false
      }).catch(() => { this.logLoading = false })
    },
    /** 查询成员管理列表 */
    getList() {
      this.loading = true
      listReader(this.queryParams).then(response => {
        this.readerList = response.rows
        this.total = response.total
      }).catch(() => {
        // 错误提示已由 request.js 拦截器统一弹出，这里只吞掉异常避免重复 toast
      }).finally(() => {
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        readerId: null,
        logOpen: false,
        logLoading: false,
        logList: [],
        logTotal: 0,
        logQuery: { pageNum: 1, pageSize: 10, cardNo: '', event: '', result: '' },
        pwdOpen: false,
        pwdSaving: false,
        pwdRow: {},
        pwdMode: 'invite',
        pwdNew: '',
        readerName: null,
        phone: null,
        email: null,
        cardNo: null,
        readerType: null,
        sex: null,
        birthDate: null,
        status: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      }
      this.resetForm("form")
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
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.readerId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加成员管理"
    },
    /** 修改按钮操作 */
    handleBorrow(row) {
      this.$router.push({ path: '/business/book-mgmt/borrow', query: { readerId: row.readerId } })
    },
    /** 重发编号：生成新编号并恢复状态 */
    handleReissue(row) {
      this.$modal.confirm('确认给《' + row.readerName + '》重发成员编号吗？将生成新编号，旧编号作废').then(() => {
        return reissueCard(row.readerId)
      }).then(res => {
        this.$modal.msgSuccess('重发成功！新成员编号：' + res.data + '（旧编号已作废）')
        this.getList()
      }).catch(() => {})
    },
    /** 重置密码：弹窗选择"发邀请邮件"或"管理员直接设密" */
    handleResetPwd(row) {
      this.pwdRow = row
      this.pwdMode = 'invite'
      this.pwdNew = ''
      this.pwdOpen = true
    },
    submitResetPwd() {
      const row = this.pwdRow
      if (this.pwdMode === 'invite') {
        this.pwdSaving = true
        resetPwdInvite(row.readerId).then(res => {
          this.$modal.msgSuccess(res.msg || '验证码已发送至登记邮箱')
          this.pwdOpen = false
        }).finally(() => { this.pwdSaving = false })
      } else {
        if (!this.pwdNew || this.pwdNew.length < 10) {
          this.$modal.msgError('新密码长度至少 10 位')
          return
        }
        this.pwdSaving = true
        setPassword({ readerId: row.readerId, newPassword: this.pwdNew }).then(() => {
          this.$modal.msgSuccess('密码已设置，该成员需用新密码重新登录')
          this.pwdOpen = false
        }).finally(() => { this.pwdSaving = false })
      }
    },
    handleUpdate(row) {
      this.reset()
      const readerId = row.readerId || this.ids
      getReader(readerId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改成员管理"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.readerId != null) {
            updateReader(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addReader(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const readerIds = row.readerId || this.ids
      this.$modal.confirm('是否确认删除成员管理编号为"' + readerIds + '"的数据项？').then(function() {
        return delReader(readerIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/reader/export', {
        ...this.queryParams
      }, `reader_${new Date().getTime()}.xlsx`)
    },
    /** 批量导入：打开弹窗 */
    handleImport() {
      this.importOpen = true
      this.importResult = null
    },
    /** 下载导入模板（全局 download 方法带登录令牌） */
    downloadTemplate() {
      this.download('system/reader/importTemplate', {}, `reader_import_template_${new Date().getTime()}.xlsx`)
    },
    /** 导入完成：展示成功/失败明细并刷新列表 */
    handleImportSuccess(res) {
      if (res.code !== 200 || !res.data) {
        this.$modal.msgError((res && res.msg) || '导入失败')
        return
      }
      this.importResult = res.data
      if (this.importResult.fail === 0) {
        this.$modal.msgSuccess('导入成功 ' + this.importResult.success + ' 条')
      } else {
        this.$modal.msgWarning('导入完成：成功 ' + this.importResult.success + ' 条，失败 ' + this.importResult.fail + ' 条，详见明细')
      }
      this.getList()
    },
    handleImportError() {
      this.$modal.msgError('上传失败，请检查文件格式（需 .xlsx）')
    }
  }
}
</script>

<style scoped>
/* 批量导入弹窗 */
.import-tip {
  font-size: 13px;
  color: #8a8a8a;
  line-height: 2;
  margin-bottom: 14px;
  background: #f7f5f0;
  border-radius: 8px;
  padding: 10px 14px;
}
.import-link {
  color: #2f6b45;
  font-weight: bold;
  cursor: pointer;
}
.import-result {
  margin-top: 14px;
}
.import-errors {
  margin-top: 10px;
  max-height: 180px;
  overflow-y: auto;
  font-size: 13px;
  color: #c65d43;
  background: #fdf0ec;
  border-radius: 8px;
  padding: 10px 14px 10px 30px;
  line-height: 1.8;
}
</style>
