<template>
  <div class="app-container sec-app">
    <!-- 顶部说明：面向非程序员 -->
    <el-alert type="info" :closable="false" show-icon title="区块 = 栏目页面上可改的文案（首屏副标语/结尾引导语）：左侧选区块，中间改内容，右侧实时预览该栏目页效果。内容留空则前台保持原样；改错了可在「历史版本」里一键回滚。首页内容请到「页面搭建」管理。" />

    <div class="sec-layout">
      <!-- 左：栏目 Tab + 区块列表 -->
      <div class="sec-left">
        <el-tabs v-model="activePage" @tab-click="onTabChange" class="block-tabs">
          <el-tab-pane v-for="p in pages" :key="p.key" :label="p.name" :name="p.key" />
        </el-tabs>
        <div v-if="!blockList.length && !loading" class="sec-empty">该栏目暂无可编辑的区块</div>
        <div v-for="b in blockList" :key="b.blockId" class="sec-item" :class="{ active: selectedId === b.blockId }" @click="select(b)">
          <div class="sec-item-top">
            <span class="sec-item-title">{{ b.title || b.blockKey }}</span>
            <el-tag size="mini" :type="b.visible === '0' ? 'success' : 'info'">{{ b.visible === '0' ? '显示中' : '已隐藏' }}</el-tag>
          </div>
          <div class="sec-item-sub">v{{ b.version }} · {{ b.updateTime || '未编辑' }}</div>
        </div>
      </div>

      <!-- 中：编辑表单 -->
      <div class="sec-mid">
        <template v-if="selectedId != null">
          <div class="sec-mid-head">
            <span class="sec-mid-title">{{ (form.title || form.blockKey) }}</span>
          </div>
          <el-alert v-if="dirty" type="warning" :closable="false" show-icon class="mb8" title="内容已修改但未保存——保存后前台预览自动刷新" />
          <el-form :model="form" label-width="80px" size="small">
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
            <el-form-item>
              <el-button type="primary" size="mini" @click="handleSave">保存（保存后预览自动刷新）</el-button>
              <el-button type="warning" plain size="mini" @click="openHistory">历史版本</el-button>
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
import { listBlock, updateBlock, listBlockHistory, rollbackBlock } from "@/api/system/cms"
import { getConfigKey } from "@/api/system/config"

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
      frontUrl: '',            // 前台地址（系统参数 site.front.url）；空/默认值 = 与后台同源，用相对路径
      previewTs: 0,
      previewLoading: true,
      historyOpen: false,
      historyList: [],
      currentBlockId: null,
      currentBlockTitle: ''
    }
  },
  computed: {
    currentPage() {
      return this.pages.find(p => p.key === this.activePage) || this.pages[0]
    },
    selected() {
      if (this.selectedId == null) return null
      return this.blockList.find(b => b.blockId === this.selectedId) || null
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
    form: {
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
    getList(withPreview) {
      this.loading = true
      listBlock({ pageNum: 1, pageSize: 100, pageKey: this.activePage }).then(response => {
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
      this.dirty = false
      this.$nextTick(() => { this._suppressDirty = false })
      this.reloadPreview()
    },
    handleSave() {
      updateBlock(this.form).then(() => {
        this.$modal.msgSuccess("已保存（历史已存档 v" + (this.form.version + 1) + "，预览已刷新）")
        this.dirty = false
        this.getList(false)
        this.reloadPreview()
      })
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
        this.getList(false)
        const row2 = this.blockList.find(b => b.blockId === this.selectedId)
        if (row2) { this._suppressDirty = true; this.form = row2; this.dirty = false; this.$nextTick(() => { this._suppressDirty = false }) }
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
.sec-left { width: 260px; flex-shrink: 0; overflow-y: auto; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 8px; }
.block-tabs { margin-bottom: 4px; }
.sec-empty { color: #909399; font-size: 13px; text-align: center; padding: 30px 0; }
.sec-item { padding: 8px 10px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; border: 1px solid transparent; }
.sec-item:hover { background: #f5f7fa; }
.sec-item.active { background: #ecf5ff; border-color: #409eff; }
.sec-item-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.sec-item-title { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sec-item-sub { color: #909399; font-size: 12px; margin-top: 2px; }
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
</style>
