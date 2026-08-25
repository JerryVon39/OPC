<template>
  <div class="app-container">
    <!-- 顶部说明：面向非程序员 -->
    <el-alert type="info" :closable="false" show-icon title="区块 = 前台页面上可改的文案：编辑保存后前台刷新即生效；内容留空则前台保持原样。改错了可在「历史版本」里一键回滚。" />

    <el-tabs v-model="activePage" @tab-click="getList">
      <el-tab-pane v-for="p in pages" :key="p.key" :label="p.name" :name="p.key" />
    </el-tabs>

    <el-collapse v-model="openKeys" v-if="blockList.length">
      <el-collapse-item v-for="b in blockList" :key="b.blockId" :name="b.blockKey">
        <template slot="title">
          <span class="block-title">{{ b.title }}</span>
          <el-tag size="mini" :type="b.visible === '0' ? 'success' : 'info'" style="margin-left:10px">{{ b.visible === '0' ? '显示中' : '已隐藏' }}</el-tag>
          <span class="block-version">v{{ b.version }} · {{ b.updateTime || '未编辑' }}</span>
        </template>
        <el-form :model="b" label-width="80px" size="small">
          <el-form-item label="标题">
            <el-input v-model="b.title" maxlength="200" placeholder="标题（对应前台加粗标题）" />
          </el-form-item>
          <el-form-item label="副标题">
            <el-input v-model="b.subtitle" maxlength="200" placeholder="副标题（未使用的区块可留空）" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="b.content" type="textarea" :rows="5" placeholder="正文文案。留空 = 前台保持原样，不会覆盖" />
          </el-form-item>
          <el-form-item label="显示">
            <el-switch v-model="b.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="mini" @click="handleSave(b)">保存（自动存档历史）</el-button>
            <el-button type="warning" plain size="mini" @click="openHistory(b)">历史版本</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>

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

export default {
  name: "CmsBlock",
  data() {
    return {
      pages: [
        { key: 'home', name: '首页' },
        { key: 'about', name: '走进社区' },
        { key: 'join', name: '入驻招商' },
        { key: 'talent', name: '人才培养' },
        { key: 'industry', name: '产业生态' }
      ],
      activePage: 'home',
      blockList: [],
      openKeys: [],
      historyOpen: false,
      historyList: [],
      currentBlockId: null,
      currentBlockTitle: ''
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      listBlock({ pageNum: 1, pageSize: 100, pageKey: this.activePage }).then(response => {
        this.blockList = response.rows || []
        this.openKeys = this.blockList.length ? [this.blockList[0].blockKey] : []
      })
    },
    handleSave(b) {
      updateBlock(b).then(() => {
        this.$modal.msgSuccess("已保存（历史已存档 v" + (b.version + 1) + "）")
        this.getList()
      })
    },
    openHistory(b) {
      this.currentBlockId = b.blockId
      this.currentBlockTitle = b.title || b.blockKey
      listBlockHistory(b.blockId).then(response => {
        this.historyList = response.data || []
        this.historyOpen = true
      })
    },
    handleRollback(row) {
      this.$modal.confirm('确认回滚到 v' + row.version + ' 吗？当前内容将替换为该版本（当前版会先存入历史）。').then(() => {
        return rollbackBlock(this.currentBlockId, row.version)
      }).then(() => {
        this.$modal.msgSuccess("已回滚")
        this.historyOpen = false
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.block-title { font-weight: 600; }
.block-version { margin-left: 12px; color: #999; font-size: 12px; }
</style>
