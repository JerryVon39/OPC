<template>
  <div class="app-container sec-app">
    <el-alert type="info" :closable="false" show-icon title="页面搭建 = 首页模块管理：左侧选模块，中间改内容，右侧实时预览前台效果。增删模块、上下移排序、开关显示都即时生效；表单内容需点「保存」后生效（保存后预览自动刷新）。" />

    <div class="sec-layout">
      <!-- 左：模块列表 -->
      <div class="sec-left">
        <div class="sec-left-head">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd" v-hasPermi="['system:cmsSection:add']">新增模块</el-button>
          <el-button type="warning" plain icon="el-icon-refresh" size="mini" @click="getList(true)">刷新</el-button>
        </div>
        <div v-if="!sectionList.length && !loading" class="sec-empty">暂无模块，点击「新增模块」开始搭建</div>
        <div v-for="(s, i) in sectionList" :key="s.sectionId" class="sec-item" :class="{ active: selectedId === s.sectionId }" @click="select(s)">
          <div class="sec-item-top">
            <span class="tmpl-tag">{{ templateName(s.template) }}</span>
            <el-switch v-model="s.visible" active-value="0" inactive-value="1" size="mini" @change="handleVisible(s)" v-hasPermi="['system:cmsSection:edit']" @click.native.stop />
          </div>
          <div class="sec-item-title">{{ s.title || s.sectionKey }}</div>
          <div class="sec-item-ops">
            <el-button size="mini" type="text" icon="el-icon-top" @click.stop="handleMove(s, 'up', i)" v-hasPermi="['system:cmsSection:sort']" :disabled="i === 0">上移</el-button>
            <el-button size="mini" type="text" icon="el-icon-bottom" @click.stop="handleMove(s, 'down', i)" v-hasPermi="['system:cmsSection:sort']" :disabled="i === sectionList.length - 1">下移</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" class="danger-text" @click.stop="handleDelete(s)" v-hasPermi="['system:cmsSection:remove']">删除</el-button>
          </div>
        </div>
      </div>

      <!-- 中：编辑表单（常驻面板） -->
      <div class="sec-mid">
        <template v-if="selectedId != null">
          <div class="sec-mid-head">
            <span class="sec-mid-title">{{ isNew ? '新增模块 · ' + templateName(form.template) : '编辑模块' }}</span>
            <el-tag v-if="isNew" size="mini" type="warning">新增草稿（保存后生效）</el-tag>
          </div>
          <el-alert v-if="dirty" type="warning" :closable="false" show-icon class="mb8" title="内容已修改但未保存——保存后前台预览自动刷新" />
          <el-form :model="form" label-width="90px" size="small">
            <el-form-item label="模块名称">
              <el-input v-model="form.title" maxlength="50" placeholder="后台列表显示的名称" />
            </el-form-item>
            <el-form-item label="显示">
              <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
            </el-form-item>

            <!-- hero：首屏文案（配置驱动，渲染器留空兜底默认） -->
            <template v-if="form.template === 'hero'">
              <el-form-item label="主标题"><el-input v-model="cfg.title" maxlength="60" placeholder="留空 = 使用默认首屏标题" /></el-form-item>
              <el-form-item label="副标题"><el-input v-model="cfg.subtitle" maxlength="100" placeholder="留空 = 使用默认副标题" /></el-form-item>
              <el-form-item label="正文"><el-input v-model="cfg.content" type="textarea" :rows="5" placeholder="留空 = 使用默认文案" /></el-form-item>
              <el-form-item label="说明"><span style="color:#999;font-size:12px">首屏轮播图请到「轮播管理」维护；文案留空则前台用默认文案。</span></el-form-item>
            </template>

            <!-- cards：卡片组 -->
            <template v-if="form.template === 'cards'">
              <el-form-item label="每行列数">
                <el-radio-group v-model="cfg.cols"><el-radio :label="2">2 列</el-radio><el-radio :label="3">3 列</el-radio></el-radio-group>
              </el-form-item>
              <el-form-item v-for="(c, i) in cfg.cards" :key="i" :label="'卡片 ' + (i + 1)">
                <div class="card-row">
                  <el-input v-model="c.icon" placeholder="图标 emoji" style="width:90px" />
                  <el-input v-model="c.title" placeholder="卡片标题" style="width:150px" />
                  <el-input v-model="c.text" type="textarea" :rows="2" placeholder="卡片正文" />
                  <el-button type="text" icon="el-icon-delete" @click="cfg.cards.splice(i, 1)">删</el-button>
                </div>
              </el-form-item>
              <el-form-item><el-button type="primary" plain size="mini" @click="cfg.cards.push({ icon: '', title: '', text: '' })">＋ 添加卡片</el-button></el-form-item>
            </template>

            <!-- tags：标签墙 -->
            <template v-if="form.template === 'tags'">
              <el-form-item v-for="(g, i) in cfg.groups" :key="i" :label="'分组 ' + (i + 1)">
                <div class="card-row">
                  <el-input v-model="g.title" placeholder="分组名（如：首批入驻企业）" style="width:220px" />
                  <el-input v-model="g.tagsText" type="textarea" :rows="2" placeholder="标签，用中文逗号分隔" />
                  <el-button type="text" icon="el-icon-delete" @click="cfg.groups.splice(i, 1)">删</el-button>
                </div>
              </el-form-item>
              <el-form-item><el-button type="primary" plain size="mini" @click="cfg.groups.push({ title: '', tagsText: '' })">＋ 添加分组</el-button></el-form-item>
            </template>

            <!-- news：新闻数量 -->
            <el-form-item v-if="form.template === 'news'" label="显示条数">
              <el-input-number v-model="cfg.count" :min="1" :max="12" />
            </el-form-item>

            <!-- timeline/contact：时间线条目 -->
            <template v-if="form.template === 'timeline' || form.template === 'contact'">
              <el-form-item v-for="(it, i) in cfg.items" :key="i" :label="'条目 ' + (i + 1)">
                <div class="card-row">
                  <el-input v-model="it.date" placeholder="日期" style="width:130px" />
                  <el-input v-model="it.title" placeholder="标题" style="width:150px" />
                  <el-input v-model="it.desc" placeholder="描述" />
                  <el-button type="text" icon="el-icon-delete" @click="cfg.items.splice(i, 1)">删</el-button>
                </div>
              </el-form-item>
              <el-form-item><el-button type="primary" plain size="mini" @click="cfg.items.push({ date: '', title: '', desc: '' })">＋ 添加条目</el-button></el-form-item>
              <el-form-item label="说明"><span style="color:#999;font-size:12px">contact 模板的联系方式自动读取站点配置（系统设置 → 参数设置）</span></el-form-item>
            </template>

            <!-- cta/text/banner_text：文案类 -->
            <template v-if="form.template === 'cta' || form.template === 'text' || form.template === 'banner_text'">
              <el-form-item label="主标题"><el-input v-model="cfg.title" placeholder="主标题" /></el-form-item>
              <el-form-item label="正文"><el-input v-model="cfg.text" type="textarea" :rows="3" placeholder="正文" /></el-form-item>
              <el-form-item label="按钮文字"><el-input v-model="cfg.btnText" placeholder="如：立即入驻" style="width:200px" /></el-form-item>
              <el-form-item label="按钮链接"><el-input v-model="cfg.btnLink" placeholder="如：join.html" style="width:300px" /></el-form-item>
              <el-form-item v-if="form.template === 'banner_text'" label="配图">
                <el-upload class="avatar-uploader" :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleImageSuccess" accept="image/*">
                  <img v-if="cfg.image" :src="imgUrl(cfg.image)" style="width:100%;max-height:120px;object-fit:cover;border-radius:6px" />
                  <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                </el-upload>
              </el-form-item>
            </template>

            <el-form-item>
              <el-button type="primary" @click="submitForm" v-hasPermi="['system:cmsSection:edit']">保存（保存后预览自动刷新）</el-button>
            </el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="点击左侧模块开始编辑，右侧实时预览对应效果" />
      </div>

      <!-- 右：实时预览 -->
      <div class="sec-right">
        <div class="preview-bar">
          <span class="preview-title">前台实时预览</span>
          <el-tag v-if="selectedId != null && !isNew" size="mini" type="success">正在定位：{{ selected ? (selected.title || selected.sectionKey) : '' }}</el-tag>
          <el-tag v-else size="mini" type="info">未选中模块</el-tag>
          <div class="preview-bar-right">
            <el-button size="mini" icon="el-icon-refresh" @click="reloadPreview">刷新预览</el-button>
            <el-button size="mini" type="text" icon="el-icon-full-screen" @click="openFront">新窗口打开前台</el-button>
          </div>
        </div>
        <div class="preview-body">
          <iframe v-if="previewSrc" :key="previewTs" :src="previewSrc" class="preview-frame" @load="previewLoading = false" />
          <div v-if="previewLoading" class="preview-mask"><i class="el-icon-loading"></i> 预览加载中…</div>
        </div>
      </div>
    </div>

    <!-- 新增：模板选择 -->
    <el-dialog title="选择模块模板" :visible.sync="addOpen" width="720px" append-to-body>
      <div class="tmpl-grid">
        <div v-for="t in templates" :key="t.value" class="tmpl-card" @click="createFromTemplate(t)">
          <div class="tmpl-icon">{{ t.icon }}</div>
          <div class="tmpl-name">{{ t.name }}</div>
          <div class="tmpl-desc">{{ t.desc }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSection, addSection, updateSection, delSection, moveSection } from "@/api/system/cms"
import { getConfigKey } from "@/api/system/config"
import { getToken } from "@/utils/auth"

export default {
  name: "CmsSection",
  data() {
    return {
      loading: false,
      sectionList: [],
      selectedId: null,      // 选中项 sectionId；'new' = 新增草稿
      isNew: false,          // 是否新增草稿态（保存走 add）
      dirty: false,          // 表单有未保存修改
      form: { sectionId: null, sectionKey: null, template: null, title: null, visible: '0', configJson: null },
      cfg: {},
      frontUrl: '',            // 前台地址（系统参数 site.front.url）；空/默认值 = 与后台同源，用相对路径
      previewTs: 0,
      previewLoading: true,
      addOpen: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      templates: [
        { value: 'hero', name: '首屏（轮播+文案）', icon: '🎠', desc: '轮播图 + 首屏标题/简介文案（轮播走轮播管理）' },
        { value: 'cards', name: '卡片组', icon: '🃏', desc: '2/3 列图标卡片（品牌理念、三大赋能、产业生态同款）' },
        { value: 'tags', name: '标签墙', icon: '🏷️', desc: '分组标签列表（企业名单、合作机构）' },
        { value: 'news', name: '新闻动态', icon: '📰', desc: '自动拉取最新新闻列表，可设条数' },
        { value: 'timeline', name: '时间线', icon: '📅', desc: '发展历程等时间节点列表' },
        { value: 'contact', name: '联系区', icon: '📮', desc: '时间线 + 联系方式（联系方式自动读站点配置）' },
        { value: 'cta', name: 'CTA 横幅', icon: '🎯', desc: '大按钮引导横幅（入驻引导同款）' },
        { value: 'text', name: '纯文本段落', icon: '📝', desc: '标题 + 长文段落' },
        { value: 'banner_text', name: '图文横幅', icon: '🖼️', desc: '配图 + 标题 + 正文 + 按钮（活动宣传用）' }
      ]
    }
  },
  computed: {
    selected() {
      if (this.isNew || this.selectedId == null) return null
      return this.sectionList.find(s => s.sectionId === this.selectedId) || null
    },
    frontBase() {
      return (this.frontUrl && this.frontUrl !== 'http://localhost') ? this.frontUrl.replace(/\/+$/, '') : ''
    },
    previewSrc() {
      const hl = this.isNew || !this.selected ? '' : '&highlight=' + encodeURIComponent(this.selected.sectionKey || '')
      return this.frontBase + '/home.html?preview=1' + hl + '&t=' + this.previewTs
    }
  },
  watch: {
    cfg: {
      deep: true,
      handler() { if (!this._suppressDirty) this.dirty = true }
    }
  },
  created() {
    this.getList(true)
    getConfigKey('site.front.url').then(res => {
      if (res && res.msg && res.msg !== 'http://localhost') this.frontUrl = res.msg
    }).catch(() => {})
  },
  methods: {
    templateName(v) {
      const t = this.templates.find(x => x.value === v)
      return t ? t.name : v
    },
    imgUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    getList(withPreview) {
      this.loading = true
      listSection({ pageNum: 1, pageSize: 100, pageKey: 'home' }).then(response => {
        this.sectionList = response.rows || []
        // 选中项可能被删除/刷新，重查后失效则清空
        if (this.selectedId != null && this.selectedId !== 'new' && !this.sectionList.some(s => s.sectionId === this.selectedId)) {
          this.selectedId = null
        }
        if (withPreview) this.reloadPreview()
      }).finally(() => { this.loading = false })
    },
    select(s) {
      this._suppressDirty = true
      this.selectedId = s.sectionId
      this.isNew = false
      this.form = s
      this.cfg = this.parseCfg(s.configJson, s.template)
      this.dirty = false
      this.$nextTick(() => { this._suppressDirty = false })
      this.reloadPreview()
    },
    openAdd() { this.addOpen = true },
    createFromTemplate(t) {
      this._suppressDirty = true
      this.addOpen = false
      this.isNew = true
      this.selectedId = 'new'
      this.form = { sectionId: null, sectionKey: 'sec-' + Date.now(), template: t.value, title: t.name, visible: '0', configJson: null }
      this.cfg = this.defaultCfg(t.value)
      this.dirty = false
      this.$nextTick(() => { this._suppressDirty = false })
      this.reloadPreview()
    },
    defaultCfg(t) {
      if (t === 'hero') return { title: '', subtitle: '', content: '' }
      if (t === 'cards') return { cols: 3, cards: [{ icon: '', title: '', text: '' }] }
      if (t === 'tags') return { groups: [{ title: '', tagsText: '' }] }
      if (t === 'news') return { count: 6 }
      if (t === 'timeline' || t === 'contact') return { items: [{ date: '', title: '', desc: '' }] }
      if (t === 'cta' || t === 'text') return { title: '', text: '', btnText: '', btnLink: '' }
      if (t === 'banner_text') return { title: '', text: '', btnText: '', btnLink: '', image: '' }
      return {}
    },
    parseCfg(json, t) {
      let cfg = this.defaultCfg(t)
      try {
        if (json) Object.assign(cfg, JSON.parse(json))
      } catch (e) { /* 配置损坏时用默认 */ }
      if (cfg.groups) cfg.groups.forEach(g => { g.tagsText = (g.tags || []).join('，') })
      return cfg
    },
    handleVisible(row) {
      updateSection({ sectionId: row.sectionId, visible: row.visible }).then(() => {
        this.$modal.msgSuccess(row.visible === '0' ? "已显示（预览已刷新）" : "已隐藏（预览已刷新）")
        this.reloadPreview()
      })
    },
    handleMove(row, dir, idx) {
      moveSection(row.sectionId, dir).then(() => { this.getList(true) })
    },
    handleImageSuccess(res) {
      if (res.code === 200) { this.cfg.image = res.fileName || res.url; this.$modal.msgSuccess("图片上传成功") }
    },
    submitForm() {
      // 序列化配置（tags 的 tagsText 转数组）
      const cfg = JSON.parse(JSON.stringify(this.cfg))
      if (cfg.groups) cfg.groups.forEach(g => { g.tags = (g.tagsText || '').split('，').map(s => s.trim()).filter(Boolean); delete g.tagsText })
      this.form.configJson = JSON.stringify(cfg)
      const isAdd = this.isNew
      if (isAdd) {
        addSection(this.form).then(() => {
          this.$modal.msgSuccess("已新增并生效（预览已刷新）")
          this.dirty = false
          // 刷新列表并选中新模块（按 sectionKey 匹配）
          listSection({ pageNum: 1, pageSize: 100, pageKey: 'home' }).then(response => {
            this.sectionList = response.rows || []
            const row = this.sectionList.find(s => s.sectionKey === this.form.sectionKey)
            this.isNew = false
            if (row) { this.selectedId = row.sectionId; this.form = row; this.cfg = this.parseCfg(row.configJson, row.template) }
            this.reloadPreview()
          })
        })
      } else {
        updateSection(this.form).then(() => {
          this.$modal.msgSuccess("已保存（预览已刷新）")
          this.dirty = false
          this.reloadPreview()
        })
      }
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除模块「' + (row.title || row.sectionKey) + '」吗？前台首页将不再显示该模块。').then(() => {
        return delSection(row.sectionId)
      }).then(() => {
        this.$modal.msgSuccess("已删除（预览已刷新）")
        if (this.selectedId === row.sectionId) this.selectedId = null
        this.getList(true)
      }).catch(() => {})
    },
    reloadPreview() {
      this.previewLoading = true
      this.previewTs = Date.now()
    },
    openFront() {
      window.open(this.frontBase + '/home.html', '_blank', 'noopener')
    }
  }
}
</script>

<style scoped>
.sec-layout { display: flex; gap: 10px; height: calc(100vh - 170px); min-height: 500px; }
.sec-left { width: 260px; flex-shrink: 0; overflow-y: auto; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 8px; }
.sec-left-head { display: flex; gap: 6px; margin-bottom: 8px; }
.sec-empty { color: #909399; font-size: 13px; text-align: center; padding: 30px 0; }
.sec-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; border: 1px solid transparent; }
.sec-item:hover { background: #f5f7fa; }
.sec-item.active { background: #ecf5ff; border-color: #409eff; }
.sec-item-top { display: flex; align-items: center; justify-content: space-between; }
.sec-item-title { font-weight: 600; margin: 4px 0 2px; font-size: 14px; }
.sec-item-ops { display: flex; }
.sec-item-ops .el-button { padding: 0; margin-right: 10px; }
.danger-text { color: #f56c6c; }
.tmpl-tag { display: inline-block; background: #ecf5ff; color: #409EFF; border-radius: 4px; padding: 1px 8px; font-size: 12px; }
.sec-mid { width: 440px; flex-shrink: 0; overflow-y: auto; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px; }
.sec-mid-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.sec-mid-title { font-weight: 600; font-size: 15px; }
.sec-right { flex: 1; display: flex; flex-direction: column; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; }
.preview-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-bottom: 1px solid #ebeef5; flex-shrink: 0; }
.preview-title { font-weight: 600; font-size: 14px; }
.preview-bar-right { margin-left: auto; display: flex; align-items: center; }
.preview-body { flex: 1; position: relative; background: #f2f3f5; }
.preview-frame { width: 100%; height: 100%; border: 0; }
.preview-mask { position: absolute; inset: 0; background: rgba(255,255,255,.65); display: flex; align-items: center; justify-content: center; color: #606266; font-size: 14px; z-index: 5; }
.tmpl-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.tmpl-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px; cursor: pointer; text-align: center; transition: all .15s; }
.tmpl-card:hover { border-color: #409EFF; box-shadow: 0 2px 10px rgba(64,158,255,.15); }
.tmpl-icon { font-size: 26px; }
.tmpl-name { font-weight: 600; margin: 6px 0 4px; }
.tmpl-desc { color: #909399; font-size: 12px; line-height: 1.5; }
.card-row { display: flex; gap: 8px; align-items: flex-start; width: 100%; }
.card-row .el-input, .card-row .el-textarea { flex: 1; }
</style>
