<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter.native="handleQuery" />
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

    <el-row :gutter="10">
      <!-- 左：栏目树 -->
      <el-col :span="4">
        <div class="category-tree">
          <div class="tree-head">
            <span class="tree-title">栏目</span>
            <el-button type="text" size="mini" icon="el-icon-plus" @click="goCategory" v-hasPermi="['system:cmsCategory:list']">管理栏目</el-button>
          </div>
          <el-tree
            :data="treeOptions"
            :props="{ label: 'categoryName', children: 'children' }"
            node-key="categoryId"
            highlight-current
            :expand-on-click-node="false"
            default-expand-all
            @node-click="handleNodeClick"
          >
            <span slot-scope="{ data }" class="tree-node">
              <span class="tree-label">{{ data.categoryName }}</span>
            </span>
          </el-tree>
        </div>
      </el-col>

      <!-- 右：文章列表 -->
      <el-col :span="20">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:cms:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-top" size="mini" :disabled="multiple" @click="handleBatchTop('1')" v-hasPermi="['system:cms:edit']">批量置顶</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-bottom" size="mini" :disabled="multiple" @click="handleBatchTop('0')" v-hasPermi="['system:cms:edit']">取消置顶</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-download" size="mini" :disabled="multiple" @click="handleBatchOffline" v-hasPermi="['system:cms:edit']">批量下线</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:cms:remove']">删除</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="封面" align="center" width="70">
            <template slot-scope="scope">
              <el-image v-if="scope.row.cover" :src="imgUrl(scope.row.cover)" style="width:52px;height:36px" fit="cover" :preview-src-list="[imgUrl(scope.row.cover)]" />
              <span v-else style="color:#999">无</span>
            </template>
          </el-table-column>
          <el-table-column label="标题" align="left" prop="title" min-width="200" show-overflow-tooltip />
          <el-table-column label="栏目" align="center" prop="categoryName" width="100">
            <template slot-scope="scope">
              {{ scope.row.categoryName || '未分类' }}
            </template>
          </el-table-column>
          <el-table-column label="所属页面" align="center" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="isPolicyCat(scope.row) ? 'warning' : 'info'">{{ isPolicyCat(scope.row) ? '政策赋能页' : '资讯动态页' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排序" align="center" width="110">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.sort" :min="0" :max="999" size="mini" controls-position="right" @change="handleSortChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="浏览量" align="center" prop="views" width="70" />
          <el-table-column label="置顶" align="center" width="70">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.isTop === '1'" type="danger" size="mini">置顶</el-tag>
              <span v-else style="color:#999">普通</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" width="85">
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
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题（必填，最长 200 字）" maxlength="200" />
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
        <el-form-item label="附件" prop="attachment">
          <file-upload v-model="form.attachment" :limit="1" accept=".pdf,.doc,.docx,.zip" />
          <div style="color:#999;font-size:12px">政策原文 PDF 等文件上传（≤20MB），前台详情页显示"下载"按钮</div>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <Editor v-model="form.content" :min-height="200" />
        </el-form-item>
        <el-form-item label="置顶" prop="isTop">
          <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="置顶" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
          <span style="color:#999;font-size:12px;margin-left:8px">越小越靠前（置顶文章之后生效）</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-if="$auth.hasPermi('system:cms:publish')" label="0">已发布</el-radio>
            <el-radio label="1">草稿</el-radio>
            <el-radio label="2">已下线</el-radio>
          </el-radio-group>
          <div style="color:#999;font-size:12px">发布后将同步展示到前台新闻动态页</div>
        </el-form-item>
        <el-form-item label="发布时间" prop="publishTime">
          <el-date-picker v-model="form.publishTime" type="datetime" placeholder="留空 = 立即发布（预约发布：填未来时间则到点才在前台展示）" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="SEO 关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="选填，多个关键词用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="SEO 描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="选填，前台详情页 meta description" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="form.articleId != null" type="warning" plain icon="el-icon-refresh-left" @click="openHistory">历史版本</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 文章历史版本弹窗（批次 A：保存前自动存档，最多 20 版） -->
    <el-dialog :title="'历史版本 · ' + historyTitle" :visible.sync="historyOpen" width="680px" append-to-body>
      <el-table :data="historyList" size="mini">
        <el-table-column label="版本" prop="version" width="70" align="center" />
        <el-table-column label="标题" prop="title" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="scope">{{ {0:'已发布',1:'草稿',2:'已下线'}[scope.row.status] || '-' }}</template>
        </el-table-column>
        <el-table-column label="更新人" prop="updateBy" width="100" />
        <el-table-column label="更新时间" prop="updateTime" width="150" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="handleRollback(scope.row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="color:#999;font-size:12px;margin-top:8px">回滚 = 恢复该版本内容（含正文/摘要/栏目/封面），回滚本身也会存为新版本。最多保留 20 个版本。</div>
    </el-dialog>
  </div>
</template>

<script>
import { listArticle, getArticle, delArticle, addArticle, updateArticle, changeArticleStatus, publishArticle, listCategory, batchTop, batchStatus, batchSort, listArticleHistory, rollbackArticle } from "@/api/system/cms"
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
      treeOptions: [],
      title: "",
      open: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      historyOpen: false,
      historyList: [],
      historyTitle: '',
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      queryParams: { pageNum: 1, pageSize: 10, title: null, categoryId: null, categoryIds: null, status: null },
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
  watch: {
    form: {
      deep: true,
      handler() { this.scheduleAutoSave() }
    }
  },
  created() {
    // 支持从栏目管理页"发文章"跳转预选栏目（?categoryId=x），并联动列表过滤
    const preset = this.$route.query.categoryId
    if (preset) {
      this.queryParams.categoryId = Number(preset)
    }
    this.loadCategoryOptions()
    this.getList()
  },
  methods: {
    /** 封面相对路径转完整地址（列表展示） */
    imgUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    loadCategoryOptions() {
      listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
        const rows = response.rows || []
        this.categoryOptions = rows
        // 树按前台页面分组：📰 资讯动态（news.html 页）📄 政策赋能（policy.html 页）
        // 组节点带 categoryIds（组内栏目列表），点击按多栏目查询
        const isPolicy = r => r.categoryName && r.categoryName.indexOf('政策') === 0
        const newsCats = rows.filter(r => !isPolicy(r))
        const policyCats = rows.filter(r => isPolicy(r))
        this.treeOptions = [{
          categoryId: 0,
          categoryName: "全部文章",
          children: [
            { categoryId: 'grp-news', categoryName: "📰 资讯动态（新闻动态页）", categoryIds: newsCats.map(r => r.categoryId), children: this.buildTree(newsCats, 0) },
            { categoryId: 'grp-policy', categoryName: "📄 政策赋能（政策赋能页）", categoryIds: policyCats.map(r => r.categoryId), children: this.buildTree(policyCats, 0) }
          ]
        }]
      })
    },
    /** 平铺栏目组装树（与栏目管理页同一逻辑，深度限制 3 级） */
    buildTree(rows, rootId, depth) {
      depth = depth || 1
      if (depth > 3) return []
      return rows
        .filter(r => r.parentId === rootId)
        .map(r => ({ ...r, children: this.buildTree(rows, r.categoryId, depth + 1) }))
    },
    handleNodeClick(node) {
      // 组节点（grp-*）：按组内栏目多选查询；其余按单栏目/全部
      if (node.categoryIds) {
        this.queryParams.categoryId = null
        this.queryParams.categoryIds = node.categoryIds.join(',')
      } else {
        this.queryParams.categoryIds = null
        this.queryParams.categoryId = node.categoryId ? node.categoryId : null
      }
      this.handleQuery()
    },
    /** 文章所属前台页面（政策类栏目 → 政策赋能页，其余 → 资讯动态页） */
    isPolicyCat(row) {
      return row.categoryName && row.categoryName.indexOf('政策') === 0
    },
    /** 跳到栏目管理页（路由 = 内容运营目录路径 content + 菜单路径 category） */
    goCategory() {
      this.$router.push('/content/category')
    },
    handleCoverSuccess(res) {
      if (res.code === 200) {
        this.form.cover = res.fileName || res.url
        this.$modal.msgSuccess("封面上传成功")
      } else {
        this.$modal.msgError("上传失败：" + (res.msg || ""))
      }
    },
    /** 封面上传前校验——仅图片、≤5MB（nginx 上限 20MB、后端 10MB） */
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
      // 预选栏目（来自栏目管理页"发文章"跳转或左侧树选择）
      const preset = this.$route.query.categoryId
      if (preset) {
        this.form.categoryId = Number(preset)
      }
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
    /** 打开文章历史版本列表 */
    openHistory() {
      this.historyTitle = this.form.title || ''
      listArticleHistory(this.form.articleId).then(response => {
        this.historyList = response.data || []
        this.historyOpen = true
      })
    },
    /** 回滚到指定历史版本 */
    handleRollback(row) {
      this.$modal.confirm('确认回滚到 v' + row.version + ' 吗？当前内容将替换为该版本（当前版会先存入历史）。').then(() => {
        return rollbackArticle(this.form.articleId, row.version)
      }).then(() => {
        this.$modal.msgSuccess("已回滚")
        this.historyOpen = false
        this.getList()
      }).catch(() => {})
    },
    /** 草稿自动保存：编辑中防抖 90 秒静默保存（仅已存在文章，避免编辑丢失） */
    scheduleAutoSave() {
      if (this.form.articleId == null || this._autoSaveTimer) return
      this._autoSaveTimer = setTimeout(() => {
        this._autoSaveTimer = null
        updateArticle(this.form).then(() => {}).catch(() => {})
      }, 90000)
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
    // 批量置顶/取消置顶
    handleBatchTop(isTop) {
      if (!this.ids.length) { this.$modal.msgWarning("请先勾选文章"); return }
      this.$modal.confirm('确认将选中的 ' + this.ids.length + ' 篇文章' + (isTop === '1' ? '置顶' : '取消置顶') + '？').then(() => {
        return batchTop(this.ids, isTop)
      }).then(() => {
        this.$modal.msgSuccess("操作成功")
        this.getList()
      }).catch(() => {})
    },
    // 批量下线（已发布 → 已下线）
    handleBatchOffline() {
      if (!this.ids.length) { this.$modal.msgWarning("请先勾选文章"); return }
      this.$modal.confirm('确认将选中的 ' + this.ids.length + ' 篇文章下线？下线后前台不再展示。').then(() => {
        return batchStatus(this.ids, '2')
      }).then(() => {
        this.$modal.msgSuccess("已下线")
        this.getList()
      }).catch(() => {})
    },
    // 列表内改排序：即时保存
    handleSortChange(row) {
      batchSort([{ articleId: row.articleId, sort: row.sort }]).then(() => {
        this.$modal.msgSuccess("排序已保存")
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
      this.$modal.confirm('确认删除该文章吗？删除后进入回收站，可在「运营辅助 → 文章回收站」恢复。').then(() => {
        return delArticle(articleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("已移入回收站")
      }).catch(() => {})
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { articleId: null, categoryId: null, title: null, summary: null, content: null, cover: null, author: null, isTop: '0', status: '0', sort: 0, attachment: null, keywords: null, description: null }
      this.resetForm("form")
    }
  }
}
</script>

<style scoped>
.category-tree {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  min-height: 400px;
}
.tree-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
}
.tree-title { font-weight: 600; font-size: 14px; }
.tree-node { display: flex; justify-content: space-between; align-items: center; flex: 1; padding-right: 6px; }
.tree-count { color: #999; font-size: 12px; }
</style>
