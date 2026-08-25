<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon title="页面搭建 = 首页模块管理：可新增/删除模块、用上下移按钮调整顺序、开关控制显示。模板是设计好的积木，填内容即可，不会破坏布局。" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd" v-hasPermi="['system:cmsSection:add']">新增模块</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="sectionList">
      <el-table-column label="顺序" align="center" width="60">
        <template slot-scope="scope"><span class="sort-num">{{ scope.row.sort }}</span></template>
      </el-table-column>
      <el-table-column label="模块" align="left" min-width="180">
        <template slot-scope="scope">
          <span class="tmpl-tag">{{ templateName(scope.row.template) }}</span>
          <span class="sec-title">{{ scope.row.title || scope.row.sectionKey }}</span>
        </template>
      </el-table-column>
      <el-table-column label="显示" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.visible" active-value="0" inactive-value="1" @change="handleVisible(scope.row)" v-hasPermi="['system:cmsSection:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="更新" align="center" prop="updateTime" width="150" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="300">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-top" @click="handleMove(scope.row, 'up')" v-hasPermi="['system:cmsSection:sort']" :disabled="scope.index === 0">上移</el-button>
          <el-button size="mini" type="text" icon="el-icon-bottom" @click="handleMove(scope.row, 'down')" v-hasPermi="['system:cmsSection:sort']" :disabled="scope.index === sectionList.length - 1">下移</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['system:cmsSection:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:cmsSection:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 编辑弹窗（按模板渲染表单） -->
    <el-dialog :title="title" :visible.sync="open" width="760px" append-to-body>
      <el-form :model="form" label-width="90px" size="small">
        <el-form-item label="模块名称">
          <el-input v-model="form.title" maxlength="50" placeholder="后台列表显示的名称" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
        </el-form-item>

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

        <el-form-item v-if="form.template === 'hero'"><span style="color:#999;font-size:12px">首屏模块 = 轮播（官网轮播管理）+ 文案（区块管理 → 首页首屏文案）。无需在此配置。</span></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSection, getSection, addSection, updateSection, delSection, moveSection } from "@/api/system/cms"
import { getToken } from "@/utils/auth"

export default {
  name: "CmsSection",
  data() {
    return {
      loading: true,
      sectionList: [],
      addOpen: false,
      open: false,
      title: "",
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      form: { sectionId: null, sectionKey: null, template: null, title: null, visible: '0', configJson: null },
      cfg: {},
      templates: [
        { value: 'hero', name: '首屏（轮播+文案）', icon: '🎠', desc: '轮播图 + 首屏标题/简介（轮播走轮播管理，文案走区块管理）' },
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
  created() { this.getList() },
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
    getList() {
      this.loading = true
      listSection({ pageNum: 1, pageSize: 100, pageKey: 'home' }).then(response => {
        this.sectionList = response.rows || []
      }).finally(() => { this.loading = false })
    },
    openAdd() { this.addOpen = true },
    createFromTemplate(t) {
      this.addOpen = false
      this.form = { sectionId: null, sectionKey: 'sec-' + Date.now(), template: t.value, title: t.name, visible: '0', configJson: null }
      this.cfg = this.defaultCfg(t.value)
      this.title = "新增模块 · " + t.name
      this.open = true
    },
    defaultCfg(t) {
      if (t === 'cards') return { cols: 3, cards: [{ icon: '', title: '', text: '' }] }
      if (t === 'tags') return { groups: [{ title: '', tagsText: '' }] }
      if (t === 'news') return { count: 6 }
      if (t === 'timeline' || t === 'contact') return { items: [{ date: '', title: '', desc: '' }] }
      if (t === 'cta' || t === 'text') return { title: '', text: '', btnText: '', btnLink: '' }
      if (t === 'banner_text') return { title: '', text: '', btnText: '', btnLink: '', image: '' }
      return {}
    },
    handleEdit(row) {
      getSection(row.sectionId).then(response => {
        this.form = response.data
        this.cfg = this.parseCfg(this.form.configJson, this.form.template)
        this.title = "编辑模块"
        this.open = true
      })
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
        this.$modal.msgSuccess(row.visible === '0' ? "已显示" : "已隐藏（前台不渲染）")
      })
    },
    handleMove(row, dir) {
      moveSection(row.sectionId, dir).then(() => { this.getList() })
    },
    handleImageSuccess(res) {
      if (res.code === 200) { this.cfg.image = res.fileName || res.url; this.$modal.msgSuccess("图片上传成功") }
    },
    submitForm() {
      // 序列化配置（tags 的 tagsText 转数组）
      const cfg = JSON.parse(JSON.stringify(this.cfg))
      if (cfg.groups) cfg.groups.forEach(g => { g.tags = (g.tagsText || '').split('，').map(s => s.trim()).filter(Boolean); delete g.tagsText })
      this.form.configJson = JSON.stringify(cfg)
      if (this.form.sectionId != null) {
        updateSection(this.form).then(() => { this.$modal.msgSuccess("已保存"); this.open = false; this.getList() })
      } else {
        addSection(this.form).then(() => { this.$modal.msgSuccess("已新增（前台刷新生效）"); this.open = false; this.getList() })
      }
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除模块「' + (row.title || row.sectionKey) + '」吗？前台首页将不再显示该模块。').then(() => {
        return delSection(row.sectionId)
      }).then(() => { this.$modal.msgSuccess("已删除"); this.getList() }).catch(() => {})
    },
    cancel() { this.open = false }
  }
}
</script>

<style scoped>
.sort-num { color: #909399; }
.tmpl-tag { display: inline-block; background: #ecf5ff; color: #409EFF; border-radius: 4px; padding: 1px 8px; font-size: 12px; margin-right: 8px; }
.sec-title { font-weight: 600; }
.tmpl-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.tmpl-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px; cursor: pointer; text-align: center; transition: all .15s; }
.tmpl-card:hover { border-color: #409EFF; box-shadow: 0 2px 10px rgba(64,158,255,.15); }
.tmpl-icon { font-size: 26px; }
.tmpl-name { font-weight: 600; margin: 6px 0 4px; }
.tmpl-desc { color: #909399; font-size: 12px; line-height: 1.5; }
.card-row { display: flex; gap: 8px; align-items: flex-start; width: 100%; }
.card-row .el-input, .card-row .el-textarea { flex: 1; }
</style>
