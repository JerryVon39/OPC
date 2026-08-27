<template>
  <div class="article-edit">
    <!-- 顶部操作条 -->
    <div class="edit-topbar">
      <div class="top-left">
        <el-button size="mini" icon="el-icon-back" @click="goBack">返回列表</el-button>
        <span class="top-title">
          {{ isEdit ? '编辑文章' : '新增文章' }}
          <span v-if="dirty" class="dirty-dot" title="有未保存修改">●</span>
        </span>
        <el-tag v-if="isEdit" size="small" :type="statusTagType">{{ statusText }}</el-tag>
        <span v-if="isEdit" class="article-id">ID: {{ articleId }}</span>
      </div>
      <div class="top-right">
        <span class="save-status" :class="{ 'save-error': saveStatusError }">{{ saveStatus }}</span>
        <el-button v-if="isEdit && previewUrl" size="mini" icon="el-icon-view" @click="openFront" title="新窗口打开前台真实页面">前台查看</el-button>
        <el-button v-if="isEdit" size="mini" type="warning" plain icon="el-icon-refresh-left" @click="openHistory">历史版本</el-button>
        <el-button size="mini" icon="el-icon-document-checked" :disabled="saving" @click="handleSave(false)" v-hasPermi="['system:cms:edit']">保存</el-button>
        <!-- P2：发布前确认（与列表页发布按钮行为一致，防手滑误发布） -->
        <el-button v-if="form.status !== '0'" size="mini" type="primary" icon="el-icon-upload2" :disabled="saving" @click="confirmPublish" v-hasPermi="['system:cms:publish']">发布</el-button>
      </div>
    </div>

    <!-- 主体：左编辑 + 右预览/设置 -->
    <div class="edit-body">
      <!-- 左主栏：标题 + 正文 -->
      <div class="edit-main">
        <el-input v-model="form.title" class="title-input" placeholder="请输入文章标题（必填，最长 200 字）" maxlength="200" size="large" clearable />
        <div class="editor-wrap">
          <Editor v-model="form.content" />
        </div>
      </div>

      <!-- 右侧栏：前台预览 / 发布设置 -->
      <div class="edit-side">
        <el-tabs v-model="sideTab" class="side-tabs">
          <el-tab-pane label="前台预览" name="preview">
            <div v-if="previewUrl" class="preview-box">
              <iframe :src="previewUrl" class="preview-frame" frameborder="0"></iframe>
              <div class="preview-bar">
                <span class="hint">预览 = 真实前台详情页；草稿/下线文章仅预览可见，发布后才对访客公开</span>
                <el-button size="mini" icon="el-icon-refresh" @click="refreshPreview">刷新</el-button>
              </div>
            </div>
            <div v-else class="preview-empty">
              <i class="el-icon-view"></i>
              <p>新增文章保存后即可预览真实前台效果</p>
              <el-button size="small" type="primary" icon="el-icon-document-checked" @click="handleSave(false)">保存并预览</el-button>
            </div>
          </el-tab-pane>
          <el-tab-pane label="发布设置" name="settings">
            <el-form ref="form" :model="form" :rules="rules" label-width="76px" size="small">
              <el-divider content-position="left">发布</el-divider>
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="form.status">
                  <el-radio v-if="$auth.hasPermi('system:cms:publish')" label="0">已发布</el-radio>
                  <el-radio label="1">草稿</el-radio>
                  <el-radio label="2">已下线</el-radio>
                </el-radio-group>
                <div class="field-tip">发布后将同步展示到前台新闻动态页</div>
              </el-form-item>
              <el-form-item label="发布时间" prop="publishTime">
                <el-date-picker v-model="form.publishTime" type="datetime" placeholder="留空 = 立即发布（预约发布：填未来时间则到点才在前台展示）" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
              </el-form-item>
              <!-- 1：定时下线——到点前台自动隐藏（活动/政策到期不用手动下架） -->
              <el-form-item label="定时下线" prop="offlineTime">
                <el-date-picker v-model="form.offlineTime" type="datetime" placeholder="留空 = 长期有效（填未来时间则到点自动从前台隐藏）" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
              </el-form-item>
              <el-form-item label="置顶" prop="isTop">
                <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="置顶" inactive-text="普通" />
              </el-form-item>
              <el-form-item label="排序" prop="sort">
                <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
                <span class="field-tip">越小越靠前（置顶文章之后生效）</span>
              </el-form-item>
              <el-divider content-position="left">基本信息</el-divider>
              <el-form-item label="栏目" prop="categoryId">
                <el-select v-model="form.categoryId" placeholder="请选择栏目（必填）" style="width:100%">
                  <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
                </el-select>
              </el-form-item>
              <el-form-item label="作者" prop="author">
                <el-input v-model="form.author" placeholder="请输入作者" />
              </el-form-item>
              <el-form-item label="摘要" prop="summary">
                <!-- P2：摘要过长前台截断且列表错位——限制 300 字 -->
                <el-input v-model="form.summary" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="前台列表展示，选填" />
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
                <div class="field-tip">可不上传封面图</div>
              </el-form-item>
              <el-form-item label="附件" prop="attachment">
                <file-upload v-model="form.attachment" :limit="1" accept=".pdf,.doc,.docx,.zip" />
                <div class="field-tip">政策原文 PDF 等文件上传（≤20MB），前台详情页显示"下载"按钮</div>
              </el-form-item>
              <el-divider content-position="left">SEO</el-divider>
              <el-form-item label="关键词" prop="keywords">
                <!-- P2：关键词用于 SEO meta，限制 200 字 -->
                <el-input v-model="form.keywords" maxlength="200" show-word-limit placeholder="选填，多个关键词用英文逗号分隔" />
              </el-form-item>
              <el-form-item label="描述" prop="description">
                <el-input v-model="form.description" type="textarea" :rows="2" placeholder="选填，前台详情页 meta description" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 文章历史版本弹窗（保存前自动存档，最多 20 版） -->
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
import { getArticle, addArticle, updateArticle, listCategory, listArticleHistory, rollbackArticle } from "@/api/system/cms"
import { getConfigKey } from "@/api/system/config"
import { getToken } from "@/utils/auth"
import Editor from "@/components/Editor"

export default {
  name: "ArticleEdit",
  components: { Editor },
  data() {
    return {
      // P0：新增默认草稿（防手滑保存即上线）；发布需显式点「发布」或右侧状态切为已发布
      form: { articleId: null, categoryId: null, title: null, summary: null, content: null, cover: null, author: null, isTop: '0', status: '1', sort: 0, attachment: null, keywords: null, description: null, offlineTime: null },
      rules: {
        title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
        categoryId: [{ required: true, message: "请选择栏目", trigger: "change" }]
      },
      categoryOptions: [],
      frontOrigin: location.origin,
      previewTs: 0,
      sideTab: 'settings',
      saveStatus: '',
      saveStatusError: false,
      dirty: false,
      saving: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      historyOpen: false,
      historyList: [],
      historyTitle: '',
      _suppressDirty: false,
      _autoSaveTimer: null
    }
  },
  computed: {
    /** URL 参数优先；新增保存后回填 form.articleId（URL 未变也可预览/编辑） */
    articleId() {
      const fromRoute = Number(this.$route.params.articleId)
      if (fromRoute > 0) return fromRoute
      return Number(this.form.articleId) || 0
    },
    isEdit() { return this.articleId > 0 },
    statusText() { return { 0: '已发布', 1: '草稿', 2: '已下线' }[this.form.status] || '未知' },
    statusTagType() { return { 0: 'success', 1: 'warning', 2: 'info' }[this.form.status] || 'info' },
    coverFullUrl() {
      const url = this.form.cover
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    /** 前台预览地址（真实 article.html 详情页；preview=1 + 后台令牌调鉴权预览接口，草稿/下线可见） */
    previewUrl() {
      if (!this.isEdit) return ''
      // 修复：token 必须取自 Cookie（若依登录态存 Cookie，utils/auth.getToken 即 Cookie 读取）；
      // 原 localStorage 读取恒为空 → 草稿/下线文章预览永远"文章不存在或未发布"
      const token = getToken() || ''
      return this.frontOrigin + '/article.html?id=' + this.articleId + '&preview=1&token=' +
        encodeURIComponent(token) + '&t=' + this.previewTs
    }
  },
  watch: {
    form: {
      deep: true,
      handler() {
        if (this._suppressDirty) return
        this.dirty = true
        this.scheduleAutoSave()
      }
    }
  },
  created() {
    // 前台地址（系统参数 site.front.url）：默认值/为空 = 与后台同源（生产 nginx 同域部署）
    getConfigKey('site.front.url').then(res => {
      if (res && res.msg && res.msg !== 'http://localhost') this.frontOrigin = res.msg
    }).catch(() => {})
    this.loadCategoryOptions()
    if (this.isEdit) {
      this.loadArticle()
      this.sideTab = 'preview'
    }
  },
  mounted() {
    window.addEventListener('beforeunload', this._onBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this._onBeforeUnload)
    this._clearAutoSave()
  },
  /** 离开页面：已存在文章且有改动 → 明示自动保存（P0：不再静默，防运营以为"离开=丢弃修改"或误判保存失败）；新增未保存 → 确认 */
  beforeRouteLeave(to, from, next) {
    if (!this.dirty) { next(); return }
    if (this.isEdit) {
      this.$modal.confirm('有未保存的修改，离开后将自动保存（不改变当前发布状态）。确定离开吗？', '未保存修改').then(() => {
        this._doAutoSave()
        next()
      }).catch(() => next(false))
      return
    }
    if (window.confirm('文章尚未保存，确定离开吗？')) { next() } else { next(false) }
  },
  methods: {
    /** 刷新/关闭页面时提示未保存（浏览器原生确认框） */
    _onBeforeUnload(e) {
      if (this.dirty) { e.returnValue = ''; return '' }
    },
    loadArticle() {
      this._suppressDirty = true
      getArticle(this.articleId).then(res => {
        this.form = res.data
        this.$nextTick(() => {
          this._suppressDirty = false
          this.dirty = false
          this.saveStatus = ''
        })
      }).catch(() => {})
    },
    loadCategoryOptions() {
      listCategory({ pageNum: 1, pageSize: 100 }).then(res => {
        this.categoryOptions = res.rows || []
      })
    },
    /** 自动保存：编辑中防抖 90 秒静默保存（仅已存在文章，防止长文编辑丢失） */
    scheduleAutoSave() {
      if (!this.isEdit || this._autoSaveTimer || this.saving) return
      this._autoSaveTimer = setTimeout(() => {
        this._autoSaveTimer = null
        this._doAutoSave()
      }, 90000)
    },
    _clearAutoSave() {
      if (this._autoSaveTimer) { clearTimeout(this._autoSaveTimer); this._autoSaveTimer = null }
    },
    _doAutoSave() {
      if (!this.isEdit || !this.dirty || this.saving) return
      this.saving = true
      this.saveStatus = '自动保存中…'
      updateArticle(this.form).then(() => {
        this.dirty = false
        this.saveStatus = '自动保存 ' + this._now()
        this.previewTs = Date.now()
      }).catch(() => {
        this.saveStatus = '自动保存失败'
        this.saveStatusError = true
      }).finally(() => { this.saving = false })
    },
    _now() {
      const d = new Date()
      return ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2)
    },
    /** P2：发布前确认（与列表页一致） */
    confirmPublish() {
      this.$modal.confirm('确认发布该文章吗？发布后前台对应页面可见。').then(() => {
        this.handleSave(true)
      }).catch(() => {})
    },
    /** 保存（forcePublish=true 时强制存为已发布；否则按右侧状态保存） */
    handleSave(forcePublish) {
      if (this.saving) return
      // 标题输入框在 el-form 之外（左主栏），rules.title 的 required 永不生效——显式校验（H9 修复）
      if (!this.form.title || !this.form.title.trim()) {
        this.$modal.msgWarning('请填写必填项：标题')
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.sideTab = 'settings'
          this.$modal.msgWarning('请填写必填项：标题、栏目')
          return
        }
        this.saving = true
        this.saveStatus = '保存中…'
        const data = { ...this.form }
        if (forcePublish) data.status = '0'
        const req = this.isEdit ? updateArticle(data) : addArticle(data)
        req.then(res => {
          // 先清 dirty 再 replace（beforeRouteLeave 检查 dirty，避免新增成功后误触发自动保存）
          this._suppressDirty = true
          this.dirty = false
          this.saveStatus = '已保存 ' + this._now()
          this.saveStatusError = false
          if (!this.isEdit && res && res.data) {
            // 新增成功：拿到新文章 ID，切到预览；URL 替换为真实 id（刷新不丢编辑态）
            this.form.articleId = res.data
            this.$router.replace('/content/article-edit/index/' + res.data)
            this.sideTab = 'preview'
          }
          this.previewTs = Date.now()
          this.$nextTick(() => { this._suppressDirty = false })
          // P0：提示明示发布状态——新增默认草稿，不再让"创建成功"误导为已上线
          this.$modal.msgSuccess(this.isEdit ? (forcePublish ? '已发布，前台可见' : '保存成功') : (this.form.status === '0' ? '创建成功并已发布，前台可见' : '已存为草稿，前台暂不展示'))
        }).catch(() => {
          // 错误提示已由 request.js 拦截器统一弹出（不重复弹窗）
          this.saveStatus = '保存失败'
          this.saveStatusError = true
        }).finally(() => { this.saving = false })
      })
    },
    refreshPreview() { this.previewTs = Date.now() },
    openFront() {
      window.open(this.frontOrigin + '/article.html?id=' + this.articleId, '_blank')
    },
    goBack() {
      if (this.dirty && !window.confirm(this.isEdit ? '有未保存的修改，确定返回列表吗？' : '文章尚未保存，确定返回列表吗？')) return
      this.$router.push('/content/article')
    },
    openHistory() {
      this.historyTitle = this.form.title || ''
      listArticleHistory(this.articleId).then(res => {
        this.historyList = res.data || []
        this.historyOpen = true
      })
    },
    /** 回滚到指定历史版本（回滚本身存为新版本） */
    handleRollback(row) {
      this.$modal.confirm('确认回滚到 v' + row.version + ' 吗？当前内容将替换为该版本（当前版会先存入历史）。').then(() => {
        return rollbackArticle(this.articleId, row.version)
      }).then(() => {
        this.$modal.msgSuccess("已回滚")
        this.historyOpen = false
        this.loadArticle()
        this.previewTs = Date.now()
      }).catch(() => {})
    },
    handleCoverSuccess(res) {
      if (res.code === 200) {
        this.form.cover = res.fileName || res.url
        this.$modal.msgSuccess("封面上传成功")
      } else {
        this.$modal.msgError("上传失败：" + (res.msg || ""))
      }
    },
    beforeImageUpload(file) {
      if (file.type.indexOf('image/') !== 0) { this.$modal.msgError("仅支持图片文件"); return false }
      if (file.size > 5 * 1024 * 1024) { this.$modal.msgError("图片大小不能超过 5MB"); return false }
      return true
    },
    handleUploadError() {
      this.$modal.msgError("上传失败，请检查网络或文件大小")
    }
  }
}
</script>

<style scoped>
.article-edit {
  height: calc(100vh - 124px);
  display: flex;
  flex-direction: column;
}
.edit-topbar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
}
.top-left { display: flex; align-items: center; gap: 10px; }
.top-title { font-size: 15px; font-weight: 600; }
.dirty-dot { color: #e6a23c; font-size: 12px; vertical-align: 2px; }
.article-id { color: #909399; font-size: 12px; }
.top-right { display: flex; align-items: center; gap: 6px; }
.save-status { color: #909399; font-size: 12px; margin-right: 4px; }
.save-status.save-error { color: #f56c6c; }

.edit-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
}
.edit-main {
  flex: 1;
  min-width: 0;
  min-height: 0; /* 必须：否则内容撑高后 .editor-wrap 的 flex:1 失效，长正文把工具栏挤出视口 */
  display: flex;
  flex-direction: column;
}
.title-input { margin-bottom: 10px; }
.editor-wrap {
  flex: 1;
  min-height: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
/* Quill 填充剩余高度（Editor 组件未设固定高时自适应）
   flex 链：.editor-wrap → .editor-root（组件根，flex:1 min-height:0）
   → .editor（height:100%）→ .ql-toolbar 固定 + .ql-container 内部滚动。
   min-height:0 必须：flex 子项默认 min-height:auto 会让长正文撑开容器
   而非内部滚动，导致工具栏被挤出视口 */
.editor-wrap >>> .editor-root { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.editor-wrap >>> .editor { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.editor-wrap >>> .ql-toolbar.ql-snow { flex: 0 0 auto; border: 0; border-bottom: 1px solid #dcdfe6; }
.editor-wrap >>> .ql-container.ql-snow { flex: 1; min-height: 0; border: 0; overflow-y: auto; }

.edit-side {
  width: 400px;
  flex: 0 0 auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.side-tabs { height: 100%; display: flex; flex-direction: column; }
.side-tabs >>> .el-tabs__header { margin: 0; padding: 0 12px; }
.side-tabs >>> .el-tabs__content { flex: 1; min-height: 0; overflow-y: auto; padding: 12px; }
.side-tabs >>> .el-tab-pane { height: 100%; }
.field-tip { color: #999; font-size: 12px; line-height: 1.6; }

.preview-box { height: 100%; display: flex; flex-direction: column; }
.preview-frame { flex: 1; width: 100%; border: 1px solid #e4e7ed; border-radius: 4px; background: #fff; }
.preview-bar { flex: 0 0 auto; display: flex; justify-content: space-between; align-items: center; padding-top: 8px; }
.preview-bar .hint { color: #909399; font-size: 12px; line-height: 1.6; }
.preview-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #909399;
}
.preview-empty i { font-size: 42px; }
.preview-empty p { margin: 0; font-size: 13px; }
</style>
