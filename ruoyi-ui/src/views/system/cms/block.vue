<template>
  <div class="app-container sec-app">
    <!-- 顶部说明：面向非程序员 -->
    <el-alert type="info" :closable="false" show-icon title="区块管理 = 全站页面内容管理（首页 + 4 个栏目页）：① 内容区块（可新增/删除/上下移，用模板填充卡片、列表、表单等）② 固定文本槽（🔒 页头副标语）。左侧选区块，中间改内容，右侧实时预览对应页面效果；保存后预览自动刷新，改错了可在「历史版本」回滚。" />

    <div class="sec-layout">
      <!-- 左：栏目 Tab + 区块列表（内容区块 / 固定槽位分区） -->
      <div class="sec-left">
        <el-tabs v-model="activePage" @tab-click="onTabChange" class="block-tabs">
          <el-tab-pane v-for="p in pages" :key="p.key" :label="p.name" :name="p.key" />
        </el-tabs>
        <div class="sec-group-head">
          <span>内容区块</span>
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd" v-hasPermi="['system:cmsBlock:add']">新增</el-button>
        </div>
        <div v-if="!contentBlocks.length && !loading" class="sec-empty">暂无内容区块，点「新增」用模板搭建栏目内容</div>
        <div v-for="(b, i) in contentBlocks" :key="b.blockId" class="sec-item" :class="{ active: selectedId === b.blockId }" @click="select(b)">
          <div class="sec-item-top">
            <span class="tmpl-tag">{{ templateName(b.template) }}</span>
            <el-switch v-model="b.visible" active-value="0" inactive-value="1" size="mini" @change="handleVisible(b)" @click.native.stop />
          </div>
          <div class="sec-item-title">{{ b.title || b.blockKey }}</div>
          <div class="sec-item-ops">
            <el-button size="mini" type="text" icon="el-icon-top" @click.stop="handleMove(b, 'up', i)" :disabled="i === 0" v-hasPermi="['system:cmsBlock:edit']">上移</el-button>
            <el-button size="mini" type="text" icon="el-icon-bottom" @click.stop="handleMove(b, 'down', i)" :disabled="i === contentBlocks.length - 1" v-hasPermi="['system:cmsBlock:edit']">下移</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" class="danger-text" @click.stop="handleDelete(b)" v-hasPermi="['system:cmsBlock:remove']">删除</el-button>
          </div>
        </div>

        <div class="sec-group-head sec-group-slot">固定文本槽 🔒</div>
        <div v-for="b in slotBlocks" :key="b.blockId" class="sec-item sec-item-slot" :class="{ active: selectedId === b.blockId }" @click="select(b)">
          <div class="sec-item-top">
            <span class="sec-item-title">{{ b.title || b.blockKey }}</span>
            <el-tag size="mini" :type="b.visible === '0' ? 'success' : 'info'">{{ b.visible === '0' ? '显示中' : '已隐藏' }}</el-tag>
          </div>
          <div class="sec-item-sub">v{{ b.version }} · {{ b.updateTime || '未编辑' }}</div>
        </div>
      </div>

      <!-- 中：编辑表单（按类型渲染：槽位 / 模板） -->
      <div class="sec-mid">
        <template v-if="selectedId != null">
          <div class="sec-mid-head">
            <span class="sec-mid-title">{{ form.title || form.blockKey }}</span>
            <el-tag size="mini" :type="isSlot ? 'info' : 'success'">{{ isSlot ? '固定文本槽' : templateName(form.template) }}</el-tag>
          </div>
          <el-alert v-if="dirty" type="warning" :closable="false" show-icon class="mb8" title="内容已修改但未保存——保存后前台预览自动刷新" />
          <el-form :model="form" label-width="90px" size="small">
            <!-- 槽位区块：标题/副标题/内容 -->
            <template v-if="isSlot">
              <el-form-item label="标题">
                <el-input v-model="form.title" maxlength="200" placeholder="标题（对应前台加粗标题）；留空 = 不覆盖" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.subtitle" maxlength="200" placeholder="副标题（未使用的区块可留空）" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input v-model="form.content" type="textarea" :rows="6" placeholder="正文文案。留空 = 前台保持原样，不会覆盖" />
              </el-form-item>
              <el-form-item label="显示">
                <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
              </el-form-item>
            </template>

            <!-- 内容区块通用：名称 + 显示 -->
            <template v-if="!isSlot">
              <el-form-item label="区块名称">
                <el-input v-model="form.title" maxlength="50" placeholder="前台显示的区块标题（如：📍 社区定位）" />
              </el-form-item>
              <el-form-item label="显示">
                <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
              </el-form-item>

              <!-- 模板字段：按注册表 schema 自动生成（blockTemplates.js） -->
              <template v-for="f in currentTpl.schema" v-if="currentTpl">
                <el-form-item :key="f.key" :label="f.label">
                  <template v-if="f.type === 'text'">
                    <el-input v-model="cfg[f.key]" :maxlength="f.maxlength" :placeholder="f.placeholder" :style="f.width ? 'width:' + f.width + 'px' : ''" />
                  </template>
                  <template v-else-if="f.type === 'textarea' || f.type === 'html'">
                    <el-input v-model="cfg[f.key]" type="textarea" :rows="f.rows || 3" :placeholder="f.placeholder" />
                  </template>
                  <template v-else-if="f.type === 'number'">
                    <el-input-number v-model="cfg[f.key]" :min="f.min" :max="f.max" />
                  </template>
                  <template v-else-if="f.type === 'radio'">
                    <el-radio-group v-model="cfg[f.key]">
                      <el-radio v-for="opt in f.options" :key="opt" :label="opt">{{ opt }} 列</el-radio>
                    </el-radio-group>
                  </template>
                  <template v-else-if="f.type === 'switch'">
                    <el-switch v-model="cfg[f.key]" :active-value="f.activeValue" :inactive-value="f.inactiveValue" :active-text="f.activeText" :inactive-text="f.inactiveText" />
                  </template>
                  <template v-else-if="f.type === 'image'">
                    <el-upload class="avatar-uploader" :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleImageSuccess" accept="image/*">
                      <img v-if="cfg.image" :src="imgUrl(cfg.image)" style="width:100%;max-height:120px;object-fit:cover;border-radius:6px" />
                      <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                    </el-upload>
                  </template>
                  <template v-else-if="f.type === 'list'">
                    <div v-for="(item, i) in cfg[f.key]" :key="i" class="card-row" style="margin-bottom:8px">
                      <el-input v-for="sf in f.fields" :key="sf.key" v-model="item[sf.key]" :placeholder="sf.placeholder" :type="sf.type === 'textarea' || sf.type === 'html' ? 'textarea' : undefined" :rows="sf.rows" :style="sf.width ? 'width:' + sf.width + 'px' : ''" />
                      <el-button type="text" icon="el-icon-delete" @click="cfg[f.key].splice(i, 1)">删</el-button>
                    </div>
                    <el-button type="primary" plain size="mini" @click="addListItem(f)">＋ 添加{{ f.itemLabel }}</el-button>
                  </template>
                </el-form-item>
              </template>
              <el-form-item v-if="currentTpl && currentTpl.tip" label="说明">
                <span style="color:#999;font-size:12px">{{ currentTpl.tip }}</span>
              </el-form-item>
            </template>

            <el-form-item>
              <el-button type="primary" @click="handleSave" v-hasPermi="['system:cmsBlock:edit']">保存（保存后预览自动刷新）</el-button>
              <el-button type="warning" plain @click="openHistory">历史版本</el-button>
            </el-form-item>
          </el-form>
        </template>
        <el-empty v-else description="点击左侧区块开始编辑，右侧实时预览对应栏目页效果" />
      </div>

      <!-- 右：实时预览 -->
      <div class="sec-right">
        <div class="preview-bar">
          <span class="preview-title">前台实时预览 · {{ currentPage.name }}</span>
          <el-tag v-if="selectedId != null" size="mini" type="success">正在定位：{{ form.blockKey }}</el-tag>
          <el-tag v-else size="mini" type="info">未选中区块</el-tag>
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

    <!-- 新增：模板选择（按当前页面场景过滤：首页 Tab 显示首页模板，栏目页 Tab 显示文档型模板） -->
    <el-dialog title="选择内容区块模板" :visible.sync="addOpen" width="720px" append-to-body>
      <div class="tmpl-grid">
        <div v-for="t in sceneTemplates" :key="t.value" class="tmpl-card" @click="createFromTemplate(t)">
          <div class="tmpl-icon">{{ t.icon }}</div>
          <div class="tmpl-name">{{ t.name }}</div>
          <div class="tmpl-desc">{{ t.desc }}</div>
          <div class="tmpl-actions">
            <el-button size="mini" type="text" icon="el-icon-view" @click.stop="openTemplatePreview(t)">预览样式</el-button>
            <el-button size="mini" type="text" icon="el-icon-plus" @click.stop="createFromTemplate(t)">使用此模板</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 模板样式预览：复用前台真实渲染器（block-preview.html），示例数据见 blockTemplates.js TEMPLATE_SAMPLES -->
    <el-dialog :title="'模板样式预览 · ' + tplPreviewName" :visible.sync="tplPreviewOpen" width="760px" append-to-body>
      <div class="tpl-preview-wrap">
        <iframe v-if="tplPreviewSrc" :key="tplPreviewTs" :src="tplPreviewSrc" class="tpl-preview-frame"></iframe>
      </div>
    </el-dialog>

    <!-- 历史版本弹窗 -->
    <el-dialog :title="'历史版本 · ' + currentBlockTitle" :visible.sync="historyOpen" width="640px" append-to-body>
      <el-table :data="historyList" size="mini">
        <el-table-column label="版本" prop="version" width="70" align="center" />
        <el-table-column label="标题" prop="title" show-overflow-tooltip />
        <el-table-column label="更新人" prop="updateBy" width="100" />
        <el-table-column label="更新时间" prop="updateTime" width="150" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="handleRollback(scope.row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="color:#999;font-size:12px;margin-top:8px">回滚 = 恢复该版本内容，回滚本身也会存为新版本，可再次回滚。最多保留 20 个版本。</div>
    </el-dialog>
  </div>
</template>

<script>
import { listBlock, addBlock, updateBlock, delBlock, moveBlock, listBlockHistory, rollbackBlock } from "@/api/system/cms"
import { getConfigKey } from "@/api/system/config"
import { getToken } from "@/utils/auth"
import { BLOCK_TEMPLATES, templateOf, defaultCfgOf, sampleCfgOf } from "./blockTemplates"

export default {
  name: "CmsBlock",
  data() {
    return {
      pages: [
        { key: 'home', name: '首页', file: 'home.html' },
        { key: 'about', name: '走进社区', file: 'about.html' },
        { key: 'join', name: '入驻招商', file: 'join.html' },
        { key: 'talent', name: '人才培养', file: 'talent.html' },
        { key: 'industry', name: '产业生态', file: 'industry.html' }
      ],
      activePage: 'about',
      loading: false,
      blockList: [],
      selectedId: null,
      dirty: false,
      form: {},
      cfg: {},
      frontUrl: '',            // 前台地址（系统参数 site.front.url）；空/默认值 = 与后台同源，用相对路径
      previewTs: 0,
      previewLoading: true,
      addOpen: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      historyOpen: false,
      historyList: [],
      currentBlockId: null,
      currentBlockTitle: '',
      tplPreviewOpen: false,
      tplPreviewSrc: '',
      tplPreviewTs: 0,
      tplPreviewName: '',
      templates: BLOCK_TEMPLATES // 模板注册表（Schema 驱动，见 blockTemplates.js）
    }
  },
  computed: {
    currentPage() {
      return this.pages.find(p => p.key === this.activePage) || this.pages[0]
    },
    contentBlocks() {
      return this.blockList.filter(b => b.template && b.template !== '')
    },
    slotBlocks() {
      // 仅显示启用中的固定文本槽（停用旧区块不展示，避免干扰）
      return this.blockList.filter(b => (!b.template || b.template === '') && b.visible === '0')
    },
    selected() {
      if (this.selectedId == null) return null
      return this.blockList.find(b => b.blockId === this.selectedId) || null
    },
    isSlot() {
      const s = this.selected
      return s ? (!s.template || s.template === '') : false
    },
    currentTpl() {
      // 当前选中内容区块的模板定义（Schema 驱动表单）
      if (this.isSlot || !this.form.template) return null
      return templateOf(this.form.template) || null
    },
    sceneTemplates() {
      // 按当前页面场景过滤模板：home Tab → 首页模板（scene ≠ page）；栏目页 Tab → 文档型（scene ≠ home）
      const isHome = this.activePage === 'home'
      return this.templates.filter(t => isHome ? t.scene !== 'page' : t.scene !== 'home')
    },
    frontBase() {
      return (this.frontUrl && this.frontUrl !== 'http://localhost') ? this.frontUrl.replace(/\/+$/, '') : ''
    },
    previewSrc() {
      const hl = this.selected ? '&highlight=' + encodeURIComponent(this.selected.blockKey || '') : ''
      return this.frontBase + '/' + this.currentPage.file + '?preview=1' + hl + '&t=' + this.previewTs
    }
  },
  watch: {
    cfg: {
      deep: true,
      handler() { if (!this._suppressDirty) this.dirty = true }
    },
    'form.title': function () { if (!this._suppressDirty) this.dirty = true },
    'form.visible': function () { if (!this._suppressDirty) this.dirty = true }
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
      return listBlock({ pageNum: 1, pageSize: 100, pageKey: this.activePage }).then(response => {
        this.blockList = response.rows || []
        if (this.selectedId != null && !this.blockList.some(b => b.blockId === this.selectedId)) {
          this.selectedId = null
        }
        if (withPreview) this.reloadPreview()
      }).finally(() => { this.loading = false })
    },
    onTabChange() {
      this.selectedId = null
      this.getList(true)
    },
    select(b) {
      this._suppressDirty = true
      this.selectedId = b.blockId
      this.form = b
      this.cfg = this.parseCfg(b.configJson, b.template)
      this.dirty = false
      this.$nextTick(() => { this._suppressDirty = false })
      this.reloadPreview()
    },
    parseCfg(json, t) {
      let cfg = this.defaultCfg(t)
      try {
        if (json) Object.assign(cfg, JSON.parse(json))
      } catch (e) { /* 配置损坏时用默认 */ }
      if (cfg.groups) cfg.groups.forEach(g => { g.tagsText = (g.tags || []).join('，') })
      return cfg
    },
    defaultCfg(t) {
      return defaultCfgOf(t) // 由模板 schema 推导
    },
    /** list 字段添加一项（按子字段 schema 生成空项） */
    addListItem(f) {
      const item = {}
      f.fields.forEach(sf => { item[sf.key] = '' })
      this.cfg[f.key].push(item)
    },
    openAdd() { this.addOpen = true },
    /** 打开模板样式预览：用示例配置调前台真实渲染器（block-preview.html），所见即前台所得 */
    openTemplatePreview(t) {
      this.tplPreviewName = t.name
      const sample = sampleCfgOf(t.value)
      this.tplPreviewSrc = this.frontBase + '/block-preview.html?template=' + encodeURIComponent(t.value) +
        '&scene=' + encodeURIComponent(t.scene) +
        '&cfg=' + encodeURIComponent(JSON.stringify(sample)) + '&t=' + Date.now()
      this.tplPreviewTs = Date.now()
      this.tplPreviewOpen = true
    },
    createFromTemplate(t) {
      this.addOpen = false
      const maxSort = this.contentBlocks.reduce((m, b) => Math.max(m, b.sort || 0), 0)
      const newBlock = {
        blockKey: 'pb-' + Date.now(),
        pageKey: this.activePage,
        title: t.name,
        template: t.value,
        configJson: JSON.stringify(this.defaultCfg(t.value)),
        sort: maxSort + 1,
        visible: '0'
      }
      addBlock(newBlock).then(() => {
        this.$modal.msgSuccess("已新增「" + t.name + "」（保存内容后前台生效）")
        this.getList(false).then(() => {
          const row = this.blockList.find(b => b.blockKey === newBlock.blockKey)
          if (row) this.select(row)
        })
      })
    },
    handleVisible(b) {
      updateBlock({ blockId: b.blockId, visible: b.visible }).then(() => {
        this.$modal.msgSuccess(b.visible === '0' ? "已显示（预览已刷新）" : "已隐藏（预览已刷新）")
        this.reloadPreview()
      })
    },
    handleMove(b, dir, idx) {
      moveBlock(b.blockId, dir).then(() => { this.getList(true) })
    },
    handleImageSuccess(res) {
      if (res.code === 200) { this.cfg.image = res.fileName || res.url; this.$modal.msgSuccess("图片上传成功") }
    },
    handleSave() {
      // 序列化配置（tags 的 tagsText 转数组）
      const cfg = JSON.parse(JSON.stringify(this.cfg))
      if (cfg.groups) cfg.groups.forEach(g => { g.tags = (g.tagsText || '').split('，').map(s => s.trim()).filter(Boolean); delete g.tagsText })
      this.form.configJson = JSON.stringify(cfg)
      updateBlock(this.form).then(() => {
        this.$modal.msgSuccess("已保存（历史已存档 v" + (this.form.version + 1) + "，预览已刷新）")
        this.dirty = false
        this.getList(false)
        this.reloadPreview()
      })
    },
    handleDelete(b) {
      this.$modal.confirm('确认删除内容区块「' + (b.title || b.blockKey) + '」吗？前台该栏目页将不再显示此区块。').then(() => {
        return delBlock(b.blockId)
      }).then(() => {
        this.$modal.msgSuccess("已删除（预览已刷新）")
        if (this.selectedId === b.blockId) this.selectedId = null
        this.getList(true)
      }).catch(() => {})
    },
    openHistory() {
      this.currentBlockId = this.form.blockId
      this.currentBlockTitle = this.form.title || this.form.blockKey
      listBlockHistory(this.form.blockId).then(response => {
        this.historyList = response.data || []
        this.historyOpen = true
      })
    },
    handleRollback(row) {
      this.$modal.confirm('确认回滚到 v' + row.version + ' 吗？当前内容将替换为该版本（当前版会先存入历史）。').then(() => {
        return rollbackBlock(this.currentBlockId, row.version)
      }).then(() => {
        this.$modal.msgSuccess("已回滚（预览已刷新）")
        this.historyOpen = false
        this.getList(false).then(() => {
          const row2 = this.blockList.find(b => b.blockId === this.selectedId)
          if (row2) { this._suppressDirty = true; this.form = row2; this.cfg = this.parseCfg(row2.configJson, row2.template); this.dirty = false; this.$nextTick(() => { this._suppressDirty = false }) }
        })
        this.reloadPreview()
      }).catch(() => {})
    },
    reloadPreview() {
      this.previewLoading = true
      this.previewTs = Date.now()
    },
    openFront() {
      window.open(this.frontBase + '/' + this.currentPage.file, '_blank', 'noopener')
    }
  }
}
</script>

<style scoped>
.sec-layout { display: flex; gap: 10px; height: calc(100vh - 170px); min-height: 500px; }
.sec-left { width: 290px; flex-shrink: 0; overflow-y: auto; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 8px; }
.block-tabs { margin-bottom: 4px; }
.sec-group-head { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 13px; color: #606266; padding: 8px 4px 6px; border-top: 1px dashed #ebeef5; margin-top: 6px; }
.sec-group-slot { border-top: 1px dashed #ebeef5; }
.sec-empty { color: #909399; font-size: 13px; text-align: center; padding: 30px 0; }
.sec-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; border: 1px solid transparent; }
.sec-item:hover { background: #f5f7fa; }
.sec-item.active { background: #ecf5ff; border-color: #409eff; }
.sec-item-slot { background: #fafbfc; }
.sec-item-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.sec-item-title { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sec-item-sub { color: #909399; font-size: 12px; margin-top: 2px; }
.sec-item-ops { display: flex; margin-top: 2px; }
.sec-item-ops .el-button { padding: 0; margin-right: 10px; }
.danger-text { color: #f56c6c; }
.tmpl-tag { display: inline-block; background: #ecf5ff; color: #409EFF; border-radius: 4px; padding: 1px 8px; font-size: 12px; flex-shrink: 0; }
.sec-mid { width: 460px; flex-shrink: 0; overflow-y: auto; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px; }
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
.tmpl-actions { margin-top: 10px; display: flex; justify-content: center; gap: 6px; }
.tpl-preview-wrap { height: 520px; }
.tpl-preview-frame { width: 100%; height: 100%; border: 1px solid #ebeef5; border-radius: 6px; background: #f2f3f5; }
.card-row { display: flex; gap: 8px; align-items: flex-start; width: 100%; }
.card-row .el-input, .card-row .el-textarea { flex: 1; }
</style>
