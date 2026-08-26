<template>
  <div class="app-container sec-app">
    <!-- 顶部说明：面向非程序员 -->
    <el-alert type="info" :closable="false" show-icon title="区块管理 = 栏目页内容管理：① 内容区块（可新增/删除/上下移，用模板填充卡片、列表、表单等）② 固定文本槽（🔒 页头副标语）。左侧选区块，中间改内容，右侧实时预览该栏目页效果；保存后预览自动刷新，改错了可在「历史版本」回滚。" />

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

              <!-- text 文本段落 -->
              <template v-if="form.template === 'text'">
                <el-form-item label="副标题"><el-input v-model="cfg.subtitle" placeholder="可选副标题" /></el-form-item>
                <el-form-item label="正文"><el-input v-model="cfg.text" type="textarea" :rows="6" placeholder="正文（支持加粗/列表等简单排版）" /></el-form-item>
              </template>

              <!-- feature 图文并排 -->
              <template v-if="form.template === 'feature'">
                <el-form-item label="配图">
                  <el-upload class="avatar-uploader" :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleImageSuccess" accept="image/*">
                    <img v-if="cfg.image" :src="imgUrl(cfg.image)" style="width:100%;max-height:120px;object-fit:cover;border-radius:6px" />
                    <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                  </el-upload>
                </el-form-item>
                <el-form-item label="正文"><el-input v-model="cfg.text" type="textarea" :rows="4" placeholder="正文" /></el-form-item>
                <el-form-item label="按钮文字"><el-input v-model="cfg.btnText" placeholder="如：了解更多" style="width:200px" /></el-form-item>
                <el-form-item label="按钮链接"><el-input v-model="cfg.btnLink" placeholder="如：join.html" style="width:300px" /></el-form-item>
                <el-form-item label="图文换向">
                  <el-switch v-model="cfg.reverse" active-value="1" inactive-value="0" active-text="图在右" inactive-text="图在左" />
                </el-form-item>
              </template>

              <!-- cards 图标卡片 -->
              <template v-if="form.template === 'cards'">
                <el-form-item label="每行列数">
                  <el-radio-group v-model="cfg.cols"><el-radio :label="2">2 列</el-radio><el-radio :label="3">3 列</el-radio></el-radio-group>
                </el-form-item>
                <el-form-item label="引导语"><el-input v-model="cfg.subtitle" placeholder="可选：卡片上方的引导说明" /></el-form-item>
                <el-form-item v-for="(c, i) in cfg.cards" :key="i" :label="'卡片 ' + (i + 1)">
                  <div class="card-row">
                    <el-input v-model="c.icon" placeholder="图标 emoji" style="width:90px" />
                    <el-input v-model="c.title" placeholder="卡片标题" style="width:150px" />
                    <el-input v-model="c.text" type="textarea" :rows="2" placeholder="卡片正文（支持简单排版）" />
                    <el-button type="text" icon="el-icon-delete" @click="cfg.cards.splice(i, 1)">删</el-button>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" plain size="mini" @click="cfg.cards.push({ icon: '', title: '', text: '' })">＋ 添加卡片</el-button></el-form-item>
              </template>

              <!-- steps 步骤清单 -->
              <template v-if="form.template === 'steps'">
                <el-form-item v-for="(st, i) in cfg.steps" :key="i" :label="'步骤 ' + (i + 1)">
                  <div class="card-row">
                    <el-input v-model="st.title" placeholder="步骤标题" style="width:200px" />
                    <el-input v-model="st.desc" placeholder="步骤说明" />
                    <el-button type="text" icon="el-icon-delete" @click="cfg.steps.splice(i, 1)">删</el-button>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" plain size="mini" @click="cfg.steps.push({ title: '', desc: '' })">＋ 添加步骤</el-button></el-form-item>
              </template>

              <!-- list 条目清单 -->
              <template v-if="form.template === 'list'">
                <el-form-item v-for="(it, i) in cfg.items" :key="i" :label="'条目 ' + (i + 1)">
                  <div class="card-row">
                    <el-input v-model="it.title" placeholder="小标题（如：咨询电话）" style="width:200px" />
                    <el-input v-model="it.desc" placeholder="描述" />
                    <el-button type="text" icon="el-icon-delete" @click="cfg.items.splice(i, 1)">删</el-button>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" plain size="mini" @click="cfg.items.push({ title: '', desc: '' })">＋ 添加条目</el-button></el-form-item>
              </template>

              <!-- tags 标签墙 -->
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

              <!-- timeline 时间线 -->
              <template v-if="form.template === 'timeline'">
                <el-form-item v-for="(it, i) in cfg.items" :key="i" :label="'节点 ' + (i + 1)">
                  <div class="card-row">
                    <el-input v-model="it.date" placeholder="日期" style="width:130px" />
                    <el-input v-model="it.title" placeholder="标题" style="width:150px" />
                    <el-input v-model="it.desc" placeholder="描述" />
                    <el-button type="text" icon="el-icon-delete" @click="cfg.items.splice(i, 1)">删</el-button>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" plain size="mini" @click="cfg.items.push({ date: '', title: '', desc: '' })">＋ 添加节点</el-button></el-form-item>
              </template>

              <!-- stats 数据亮点 -->
              <template v-if="form.template === 'stats'">
                <el-form-item v-for="(st, i) in cfg.items" :key="i" :label="'数据 ' + (i + 1)">
                  <div class="card-row">
                    <el-input v-model="st.value" placeholder="数值（如：21 天）" style="width:130px" />
                    <el-input v-model="st.label" placeholder="说明（如：从签约到揭牌）" style="width:220px" />
                    <el-button type="text" icon="el-icon-delete" @click="cfg.items.splice(i, 1)">删</el-button>
                  </div>
                </el-form-item>
                <el-form-item><el-button type="primary" plain size="mini" @click="cfg.items.push({ value: '', label: '' })">＋ 添加数据</el-button></el-form-item>
                <el-form-item label="补充说明"><el-input v-model="cfg.text" type="textarea" :rows="2" placeholder="可选：数据下方补充文字" /></el-form-item>
              </template>

              <!-- quote 金句引用 -->
              <template v-if="form.template === 'quote'">
                <el-form-item label="金句"><el-input v-model="cfg.text" type="textarea" :rows="3" placeholder="引语内容" /></el-form-item>
                <el-form-item label="出处"><el-input v-model="cfg.author" placeholder="如：社区运营理念" style="width:260px" /></el-form-item>
              </template>

              <!-- cta 横幅 -->
              <template v-if="form.template === 'cta'">
                <el-form-item label="主标题"><el-input v-model="cfg.title" placeholder="留空则用区块名称" /></el-form-item>
                <el-form-item label="正文"><el-input v-model="cfg.text" type="textarea" :rows="2" placeholder="引导文案" /></el-form-item>
                <el-form-item label="按钮文字"><el-input v-model="cfg.btnText" placeholder="如：立即入驻" style="width:200px" /></el-form-item>
                <el-form-item label="按钮链接"><el-input v-model="cfg.btnLink" placeholder="如：join.html" style="width:300px" /></el-form-item>
              </template>

              <!-- form 申请表单 -->
              <el-form-item v-if="form.template === 'form'" label="说明">
                <span style="color:#999;font-size:12px">入驻申请表单（字段固定：名称/联系人/邮箱/说明），提交后由运营团队处理。可在此调整区块名称与显示状态。</span>
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

    <!-- 新增：模板选择 -->
    <el-dialog title="选择内容区块模板" :visible.sync="addOpen" width="720px" append-to-body>
      <div class="tmpl-grid">
        <div v-for="t in templates" :key="t.value" class="tmpl-card" @click="createFromTemplate(t)">
          <div class="tmpl-icon">{{ t.icon }}</div>
          <div class="tmpl-name">{{ t.name }}</div>
          <div class="tmpl-desc">{{ t.desc }}</div>
        </div>
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

export default {
  name: "CmsBlock",
  data() {
    return {
      pages: [
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
      templates: [
        { value: 'text', name: '文本段落', icon: '📝', desc: '标题 + 长文段落（支持简单排版）' },
        { value: 'feature', name: '图文并排', icon: '🖼️', desc: '配图 + 标题 + 正文 + 按钮，可左右换向' },
        { value: 'cards', name: '图标卡片', icon: '🃏', desc: '2/3 列图标卡片（价值点/课程/企业）' },
        { value: 'steps', name: '步骤清单', icon: '🔢', desc: '①②③ 编号步骤（入驻流程/申报流程）' },
        { value: 'list', name: '条目清单', icon: '📋', desc: '圆点列表，每条小标题+描述（政策/权益）' },
        { value: 'tags', name: '标签墙', icon: '🏷️', desc: '分组标签列表（机构/合作方）' },
        { value: 'timeline', name: '时间线', icon: '📅', desc: '时间节点列表（发展历程）' },
        { value: 'stats', name: '数据亮点', icon: '📊', desc: '2-4 个数字+说明（实力展示）' },
        { value: 'quote', name: '金句引用', icon: '💬', desc: '大字号引语 + 出处' },
        { value: 'cta', name: 'CTA 横幅', icon: '🎯', desc: '大按钮引导横幅（页尾引导）' },
        { value: 'form', name: '申请表单', icon: '📮', desc: '入驻申请表单（join 页专用，字段固定）' }
      ]
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
      return this.blockList.filter(b => !b.template || b.template === '')
    },
    selected() {
      if (this.selectedId == null) return null
      return this.blockList.find(b => b.blockId === this.selectedId) || null
    },
    isSlot() {
      const s = this.selected
      return s ? (!s.template || s.template === '') : false
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
      if (t === 'text') return { subtitle: '', text: '' }
      if (t === 'feature') return { image: '', text: '', btnText: '', btnLink: '', reverse: '0' }
      if (t === 'cards') return { cols: 3, subtitle: '', cards: [{ icon: '', title: '', text: '' }] }
      if (t === 'steps') return { steps: [{ title: '', desc: '' }] }
      if (t === 'list') return { items: [{ title: '', desc: '' }] }
      if (t === 'tags') return { groups: [{ title: '', tagsText: '' }] }
      if (t === 'timeline') return { items: [{ date: '', title: '', desc: '' }] }
      if (t === 'stats') return { items: [{ value: '', label: '' }], text: '' }
      if (t === 'quote') return { text: '', author: '' }
      if (t === 'cta') return { title: '', text: '', btnText: '', btnLink: '' }
      return {}
    },
    openAdd() { this.addOpen = true },
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
.card-row { display: flex; gap: 8px; align-items: flex-start; width: 100%; }
.card-row .el-input, .card-row .el-textarea { flex: 1; }
</style>
