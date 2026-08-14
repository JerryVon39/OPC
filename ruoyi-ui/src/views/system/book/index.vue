<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="图书名称" prop="bookName">
        <el-input
          v-model="queryParams.bookName"
          placeholder="请输入图书名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入作者"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="出版社" prop="publisher">
        <el-input
          v-model="queryParams.publisher"
          placeholder="请输入出版社"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="图书类型" prop="bookType">
        <el-select
          v-model="queryParams.bookType"
          placeholder="请选择图书类型"
          clearable
          @keyup.enter.native="handleQuery"
        >
          <el-option
            v-for="dict in bookTypeOptions"
            :key="dict.dictValue"
            :label="dict.dictLabel"
            :value="dict.dictValue"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="在架" value="0" />
          <el-option label="下架" value="1" />
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
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:book:add']"
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
          v-hasPermi="['system:book:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:book:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['system:book:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="图书ID" align="center" prop="bookId" width="70" />
      <el-table-column label="封面" align="center" width="80">
        <template slot-scope="scope">
          <el-image v-if="scope.row.cover" :src="imgUrl(scope.row.cover)" style="width:44px;height:58px" fit="cover" :preview-src-list="[imgUrl(scope.row.cover)]" />
          <span v-else>📕</span>
        </template>
      </el-table-column>
      <el-table-column label="图书名称" align="center" prop="bookName" min-width="140" />
      <el-table-column label="作者" align="center" prop="author" />
      <el-table-column label="图书类型" align="center" prop="bookType" width="80">
        <template slot-scope="scope">
          <dict-tag :options="bookTypeOptions" :value="scope.row.bookType" />
        </template>
      </el-table-column>
      <el-table-column label="出版社" align="center" prop="publisher" />
      <el-table-column label="价格(元)" align="center" prop="price" />
      <el-table-column label="库存数量" align="center" width="100">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.stock <= warnThreshold" type="danger" size="mini">仅剩 {{ scope.row.stock }} 本</el-tag>
          <span v-else>{{ scope.row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">在架</el-tag>
          <el-tag v-else type="info" size="mini">下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-reading" @click="handleBorrow(scope.row)">借阅历史</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:book:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:book:remove']"
          >删除</el-button>
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

    <!-- 添加或修改图书信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="图书名称" prop="bookName">
              <el-input v-model="form.bookName" placeholder="请输入图书名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图书类型" prop="bookType">
              <el-select v-model="form.bookType" placeholder="请选择图书类型" style="width:100%">
                <el-option
                  v-for="dict in bookTypeOptions"
                  :key="dict.dictValue"
                  :label="dict.dictLabel"
                  :value="dict.dictValue"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格(元)" prop="price">
              <el-input v-model="form.price" placeholder="请输入价格(元)" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版日期" prop="publishDate">
              <el-date-picker clearable
                v-model="form.publishDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出版日期"
                style="width:100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input v-model="form.stock" placeholder="请输入库存数量" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">在架</el-radio>
                <el-radio label="1">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入 ISBN 书号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图片" prop="cover">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleCoverSuccess"
              >
                <img v-if="form.cover" :src="coverFullUrl" class="cover-preview" />
                <i v-else class="el-icon-plus avatar-uploader-icon"></i>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图书简介" prop="intro">
              <div class="bbcode-toolbar">
                <el-button size="mini" @click="insertBbcode('[b]','[/b]')">B</el-button>
                <el-button size="mini" @click="insertBbcode('[i]','[/i]')">I</el-button>
                <el-button size="mini" @click="insertBbcode('[u]','[/u]')">U</el-button>
                <el-button size="mini" @click="insertBbcodeColor">🎨 颜色</el-button>
                <el-button size="mini" @click="insertBbcodeUrl">🔗 链接</el-button>
                <el-button size="mini" @click="insertBbcode('[quote]','[/quote]')">📦 引用</el-button>
                <el-button size="mini" @click="insertBbcode('[center]','[/center]')">居中</el-button>
                <el-button size="mini" @click="clearBbcode">✂️ 清除格式</el-button>
              </div>
              <el-input ref="introInput" v-model="form.intro" type="textarea" :rows="4" placeholder="支持 BBCODE：如 [b]粗体[/b] [quote]引用[/quote] [color=red]红字[/color] [url=https://x]链接[/url]" />
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
import { listBook, getBook, delBook, addBook, updateBook } from "@/api/system/book"
import { getToken } from "@/utils/auth"
import { getDicts } from "@/api/system/dict/data"
import { getConfigKey } from "@/api/system/config"

export default {
  name: "Book",
  data() {
    return {
      // book type dict
      bookTypeOptions: [],
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
      // 图书信息表格数据
      bookList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 封面上传地址与请求头（携带登录令牌）
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      // 库存预警阈值（系统参数 book.stock.warn，后台参数设置可改）
      warnThreshold: 3,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        bookName: null,
        author: null,
        bookType: null,
        publisher: null,
        price: null,
        publishDate: null,
        stock: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        bookName: [
          { required: true, message: "图书名称不能为空", trigger: "blur" }
        ],
      }
    }
  },
  computed: {
    // 封面上传预览：相对路径需拼接口前缀才能显示
    coverFullUrl() {
      return this.imgUrl(this.form.cover)
    }
  },
  created() {
    this.getDicts("book_type").then(response => {
      this.bookTypeOptions = response.data;
    });
    // 读取库存预警阈值参数（RuoYi 该接口把参数值放在 msg 字段）
    getConfigKey('book.stock.warn').then(res => {
      const v = parseInt(res.msg, 10)
      if (!isNaN(v)) this.warnThreshold = v
    })
    this.getList()
  },
  methods: {
    /** 封面相对路径转完整地址（http 开头的不处理） */
    imgUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith('/dev-api')) return url
      return process.env.VUE_APP_BASE_API + url
    },
    /** 查询图书信息列表 */
    getList() {
      this.loading = true
      listBook(this.queryParams).then(response => {
        this.bookList = response.rows
        this.total = response.total
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
        bookId: null,
        bookName: null,
        author: null,
        bookType: null,
        publisher: null,
        price: null,
        publishDate: null,
        stock: null,
        status: '0',
        remark: null,
        cover: null,
        isbn: null,
        intro: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      }
      this.resetForm("form")
    },
    /** BBCODE 快捷插入（光标处） */
    getIntroTextarea() {
      return this.$refs.introInput ? this.$refs.introInput.$refs.textarea : null
    },
    insertBbcode(prefix, suffix) {
      const ta = this.getIntroTextarea()
      const val = this.form.intro || ''
      if (!ta) { this.form.intro = val + prefix + suffix; return }
      const start = ta.selectionStart, end = ta.selectionEnd
      const selected = val.substring(start, end)
      this.form.intro = val.substring(0, start) + prefix + selected + suffix + val.substring(end)
      this.$nextTick(() => {
        ta.focus()
        ta.setSelectionRange(start + prefix.length, start + prefix.length + selected.length)
      })
    },
    insertBbcodeColor() {
      this.$prompt('输入颜色（如 red 或 #c65d43）', '颜色', { inputValue: '#c65d43', inputPattern: /^[a-zA-Z0-9#]{3,7}$/, inputErrorMessage: '格式不正确' }).then(({ value }) => {
        this.insertBbcode('[color=' + value + ']', '[/color]')
      }).catch(() => {})
    },
    insertBbcodeUrl() {
      this.$prompt('输入链接地址（http/https）', '链接', { inputValue: 'https://', inputPattern: /^(https?|#)\S+$/, inputErrorMessage: '仅支持 http/https' }).then(({ value }) => {
        this.insertBbcode('[url=' + value + ']', '[/url]')
      }).catch(() => {})
    },
    clearBbcode() {
      const ta = this.getIntroTextarea()
      if (!ta) return
      this.form.intro = ta.value
        .replace(/\[(b|i|u|quote|center|code)\]/g, '').replace(/\[\/(b|i|u|quote|center|code)\]/g, '')
        .replace(/\[(color|size|url|img)=[^\]]*\]/g, '').replace(/\[\/(color|size|url|img)\]/g, '')
    },
    /** 封面上传成功回调（存相对路径 fileName，通过 /dev-api 代理加载，换端口/部署不写死主机名） */
    handleCoverSuccess(res) {
      if (res.code === 200) {
        this.form.cover = res.fileName || res.url
        this.$modal.msgSuccess("封面上传成功")
      } else {
        this.$modal.msgError("上传失败：" + (res.msg || ""))
      }
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
      this.ids = selection.map(item => item.bookId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加图书信息"
    },
    /** 跳转借阅历史（带图书ID过滤；路由由菜单生成：图书业务目录business下） */
    handleBorrow(row) {
      this.$router.push({ path: '/business/borrow', query: { bookId: row.bookId } })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const bookId = row.bookId || this.ids
      getBook(bookId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改图书信息"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.bookId != null) {
            updateBook(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBook(this.form).then(response => {
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
      const bookIds = row.bookId || this.ids
      this.$modal.confirm('是否确认删除图书信息编号为"' + bookIds + '"的数据项？').then(function() {
        return delBook(bookIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/book/export', {
        ...this.queryParams
      }, `book_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.cover-preview {
  width: 100px;
  height: 140px;
  object-fit: cover;
  display: block;
  border-radius: 6px;
}
.bbcode-toolbar {
  margin-bottom: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.bbcode-toolbar .el-button--mini {
  padding: 4px 8px;
  font-weight: bold;
}
</style>
