<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="栏目" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择栏目" clearable>
          <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="已发布" value="0" />
          <el-option label="草稿" value="1" />
          <el-option label="已下线" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:cms:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:cms:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标题" align="left" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="栏目" align="center" prop="categoryName" width="100">
        <template slot-scope="scope">
          {{ scope.row.categoryName || '未分类' }}
        </template>
      </el-table-column>
      <el-table-column label="作者" align="center" prop="author" width="120" show-overflow-tooltip />
      <el-table-column label="浏览量" align="center" prop="views" width="80" />
      <el-table-column label="置顶" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isTop === '1'" type="danger" size="mini">置顶</el-tag>
          <span v-else style="color:#999">普通</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">已发布</el-tag>
          <el-tag v-else-if="scope.row.status === '1'" type="warning" size="mini">草稿</el-tag>
          <el-tag v-else type="info" size="mini">已下线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" prop="publishTime" width="150" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="230">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:cms:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-top" @click="handleTop(scope.row)" v-hasPermi="['system:cms:edit']">{{ scope.row.isTop === '1' ? '取消置顶' : '置顶' }}</el-button>
          <el-button v-if="scope.row.status !== '0'" size="mini" type="text" icon="el-icon-upload2" @click="handlePublish(scope.row)" v-hasPermi="['system:cms:publish']">发布</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-download" @click="handleOffline(scope.row)" v-hasPermi="['system:cms:edit']">下线</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:cms:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题（必填）" />
        </el-form-item>
        <el-form-item label="栏目" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择栏目（必填）" style="width:100%">
            <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入摘要（前台列表展示，选填）" />
        </el-form-item>
        <el-form-item label="封面" prop="cover">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            accept="image/*"
            :before-upload="beforeImageUpload"
            :on-error="handleUploadError"
          >
            <img v-if="form.cover" :src="coverFullUrl" style="width:100%;max-height:140px;object-fit:cover;border-radius:6px" />
            <i v-else class="el-icon-plus avatar-uploader-icon" style="width:100%"></i>
          </el-upload>
          <div style="color:#999;font-size:12px">可不上传封面图</div>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <Editor v-model="form.content" :min-height="200" />
        </el-form-item>
        <el-form-item label="置顶" prop="isTop">
          <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="置顶" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-if="$auth.hasPermi('system:cms:publish')" label="0">已发布</el-radio>
            <el-radio label="1">草稿</el-radio>
            <el-radio label="2">已下线</el-radio>
          </el-radio-group>
          <div style="color:#999;font-size:12px">发布后将同步展示到前台新闻动态页</div>
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
import { listArticle, getArticle, delArticle, addArticle, updateArticle, changeArticleStatus, publishArticle, listCategory } from "@/api/system/cms"
import { getToken } from "@/utils/auth"

export default {
  name: "CmsArticle",
  data() {
    return {
      loading: true,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      articleList: [],
      categoryOptions: [],
      title: "",
      open: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      queryParams: { pageNum: 1, pageSize: 10, title: null, categoryId: null, status: null },
      form: {},
      rules: {
        title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
        categoryId: [{ required: true, message: "请选择栏目", trigger: "change" }]
      }
    }
  },
  computed: {
    coverFullUrl() {
      const url = this.form.cover
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    }
  },
  created() {
    this.loadCategoryOptions()
    this.getList()
  },
  methods: {
    loadCategoryOptions() {
      listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
        this.categoryOptions = response.rows || []
      })
    },
    handleCoverSuccess(res) {
      if (res.code === 200) {
        this.form.cover = res.fileName || res.url
        this.$modal.msgSuccess("封面上传成功")
      } else {
        this.$modal.msgError("上传失败：" + (res.msg || ""))
      }
    },
    /** M4：封面上传前校验——仅图片、≤5MB（nginx 上限 20MB、后端 10MB） */
    beforeImageUpload(file) {
      if (file.type.indexOf('image/') !== 0) { this.$modal.msgError("仅支持图片文件"); return false }
      if (file.size > 5 * 1024 * 1024) { this.$modal.msgError("图片大小不能超过 5MB"); return false }
      return true
    },
    handleUploadError() {
      this.$modal.msgError("上传失败，请检查网络或文件大小")
    },
    getList() {
      this.loading = true
      listArticle(this.queryParams).then(response => {
        this.articleList = response.rows
        this.total = response.total
      }).catch(() => {
        // 错误提示已由 request.js 拦截器统一弹出
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.articleId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增文章"
    },
    handleUpdate(row) {
      this.reset()
      getArticle(row.articleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改文章"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.articleId != null) {
            updateArticle(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addArticle(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    // 置顶/取消置顶（仅更新 isTop 字段，其余字段不受影响）
    handleTop(row) {
      const isTop = row.isTop === '1' ? '0' : '1'
      updateArticle({ articleId: row.articleId, isTop: isTop }).then(() => {
        this.$modal.msgSuccess(isTop === '1' ? "置顶成功" : "已取消置顶")
        this.getList()
      })
    },
    // 发布：草稿/已下线 → 已发布（首次发布自动写入发布时间）
    handlePublish(row) {
      this.$modal.confirm('确认发布该文章吗？发布后前台新闻动态页可见。').then(() => {
        return publishArticle(row.articleId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("发布成功")
      }).catch(() => {})
    },
    // 下线：已发布 → 已下线（前台不再展示）
    handleOffline(row) {
      this.$modal.confirm('确认将该文章下线吗？下线后前台不再展示。').then(() => {
        return changeArticleStatus(row.articleId, '2')
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("已下线")
      }).catch(() => {})
    },
    handleDelete(row) {
      const articleIds = row.articleId || this.ids
      this.$modal.confirm('确认删除该文章吗？').then(() => {
        return delArticle(articleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { articleId: null, categoryId: null, title: null, summary: null, content: null, cover: null, author: null, isTop: '0', status: '0' }
      this.resetForm("form")
    }
  }
}
</script>
