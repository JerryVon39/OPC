<template>
  <div class="app-container sec-app">
    <!-- 顶部说明：面向非程序员 -->
    <el-alert type="info" :closable="false" show-icon title="区块管理 = 全站页面内容管理（首页 + 4 个栏目页）：① 内容区块（可新增/删除/上下移，用模板填充卡片、列表、表单等）② 固定文本槽（🔒 页头副标语）。左侧选区块，中间改内容，右侧实时预览对应页面效果；保存后预览自动刷新，改错了可在「历史版本」回滚。" />

    <!-- ===== ① 页面选择视图（卡片网格）===== -->
    <div v-if="viewMode === 'pages'" class="page-picker">
      <div class="picker-head">
        <span class="picker-title">选择要编辑的页面</span>
        <div>
          <el-button type="primary" plain icon="el-icon-document-add" size="mini" @click="openPageDlg" v-hasPermi="['system:cmsBlock:add']">＋ 新增页面</el-button>
          <el-button type="text" icon="el-icon-setting" size="mini" @click="pageMgrOpen = true" v-hasPermi="['system:cmsBlock:edit']">管理自定义页面</el-button>
        </div>
      </div>
      <div class="page-grid">
        <div v-for="p in pages" :key="p.key" class="page-card" @click="goBlocks(p)">
          <div class="page-card-name">{{ p.name }}</div>
          <div class="page-card-sub">
            <template v-if="p.custom">自定义页 · page.html?key={{ p.key }}</template>
            <template v-else>内置页面 · {{ p.file }}</template>
          </div>
          <div class="page-card-count">{{ pageBlockCount(p.key) }} 个区块</div>
          <div class="page-card-enter">进入编辑 →</div>
        </div>
      </div>
    </div>

    <!-- ===== ② 区块列表视图（整页）===== -->
    <div v-else-if="viewMode === 'blocks'" class="blocks-view">
      <div class="blocks-head">
        <el-button type="text" icon="el-icon-back" @click="goPages">返回页面选择</el-button>
        <span class="blocks-title">📄 {{ currentPage.name }} · 区块列表</span>
        <div class="blocks-ops">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openAdd" v-hasPermi="['system:cmsBlock:add']">＋ 新增区块</el-button>
          <el-button type="text" icon="el-icon-setting" size="mini" @click="pageMgrOpen = true" v-hasPermi="['system:cmsBlock:edit']">页面管理</el-button>
        </div>
      </div>
      <div class="blocks-body">
        <div class="blocks-group">
          <div class="blocks-group-head">内容区块 <span class="blocks-count">({{ contentBlocks.length }})</span></div>
          <div v-if="!contentBlocks.length && !loading" class="sec-empty">暂无内容区块，点「＋ 新增区块」用模板搭建页面内容</div>
          <div v-for="(b, i) in contentBlocks" :key="b.blockId" class="block-card" @click="select(b)">
            <div class="block-card-main">
              <span class="tmpl-tag">{{ templateName(b.template) }}</span>
              <span class="block-card-title">{{ b.title || b.blockKey }}</span>
              <el-switch v-model="b.visible" active-value="0" inactive-value="1" size="mini" @change="handleVisible(b)" @click.native.stop />
            </div>
            <div class="block-card-ops">
              <el-button size="mini" type="text" icon="el-icon-edit" @click.stop="select(b)">编辑</el-button>
              <el-button size="mini" type="text" icon="el-icon-top" @click.stop="handleMove(b, 'up', i)" :disabled="i === 0" v-hasPermi="['system:cmsBlock:edit']">上移</el-button>
              <el-button size="mini" type="text" icon="el-icon-bottom" @click.stop="handleMove(b, 'down', i)" :disabled="i === contentBlocks.length - 1" v-hasPermi="['system:cmsBlock:edit']">下移</el-button>
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click.stop="handleCopy(b)" v-hasPermi="['system:cmsBlock:add']">复制</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" class="danger-text" @click.stop="handleDelete(b)" v-hasPermi="['system:cmsBlock:remove']">删除</el-button>
            </div>
          </div>
        </div>
        <div class="blocks-group">
          <div class="blocks-group-head">固定文本槽 <span class="blocks-count">({{ slotBlocks.length }})</span></div>
          <div v-for="b in slotBlocks" :key="b.blockId" class="block-card block-card-slot" @click="select(b)">
            <div class="block-card-main">
              <span class="block-card-title">🔒 {{ b.title || b.blockKey }}</span>
              <el-tag size="mini" :type="b.visible === '0' ? 'success' : 'info'">{{ b.visible === '0' ? '显示中' : '已隐藏' }}</el-tag>
            </div>
            <div class="block-card-sub">v{{ b.version }} · {{ b.updateTime || '未编辑' }} · 点击编辑</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== ③ 编辑视图（表单 + 实时预览双栏）===== -->
    <div v-else ref="layout" class="edit-layout">
      <div class="edit-head">
        <el-button type="text" icon="el-icon-back" @click="goBlocksBack">返回区块列表</el-button>
        <span class="edit-title">{{ currentPage.name }} · {{ form.title || form.blockKey || '未选择区块' }}</span>
        <span v-if="selectedId != null" class="edit-tag"><el-tag size="mini" :type="isSlot ? 'info' : 'success'">{{ isSlot ? '固定文本槽' : templateName(form.template) }}</el-tag></span>
        <div class="edit-ops">
          <el-button v-if="selectedId != null" size="mini" type="primary" @click="handleSave" v-hasPermi="['system:cmsBlock:edit']">保存</el-button>
          <el-button v-if="selectedId != null" size="mini" type="warning" plain @click="openHistory">历史版本</el-button>
          <el-button v-if="selectedId != null" size="mini" icon="el-icon-view" @click="previewCurrent">预览当前效果</el-button>
          <el-button v-if="selectedId != null" size="mini" :icon="editFull ? 'el-icon-close' : 'el-icon-full-screen'" @click="toggleFull">{{ editFull ? '退出放大' : '放大编辑' }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" @click="reloadPreview">刷新预览</el-button>
          <el-button size="mini" type="text" icon="el-icon-full-screen" @click="openFront">新窗口打开前台</el-button>
        </div>
      </div>
      <div class="edit-body">
        <!-- 编辑表单（flex 弹性宽） -->
        <div class="edit-form-area">
          <template v-if="selectedId != null">
            <el-alert v-if="dirty" type="warning" :closable="false" show-icon class="mb8" title="内容已修改但未保存——保存后前台预览自动刷新" />
            <el-form :model="form" label-width="90px" size="small">
              <template v-if="isSlot">
                <el-form-item label="标题">
                  <el-input v-model="form.title" maxlength="200" placeholder="标题（对应前台加粗标题）；留空 = 不覆盖" />
                </el-form-item>
                <el-form-item label="副标题">
                  <el-input v-model="form.subtitle" maxlength="200" placeholder="副标题（未使用的区块可留空）" />
                </el-form-item>
                <el-form-item label="内容">
                  <el-input v-model="form.content" type="textarea" :rows="editFull ? 14 : 8" placeholder="正文文案。留空 = 前台保持原样，不会覆盖" />
                </el-form-item>
                <el-form-item label="显示">
                  <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
                </el-form-item>
              </template>

              <template v-if="!isSlot">
                <el-form-item label="区块名称">
                  <el-input v-model="form.title" maxlength="50" placeholder="前台显示的区块标题（如：📍 社区定位）" />
                </el-form-item>
                <el-form-item label="显示">
                  <el-switch v-model="form.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
                </el-form-item>

                <template v-for="f in currentTpl.schema" v-if="currentTpl">
                  <el-form-item :key="f.key" :label="f.label">
                    <template v-if="f.type === 'text'">
                      <el-input v-model="cfg[f.key]" :maxlength="f.maxlength" :placeholder="f.placeholder" :style="f.width ? 'width:' + f.width + 'px' : ''" />
                    </template>
                    <template v-else-if="f.type === 'textarea'">
                      <el-input v-model="cfg[f.key]" type="textarea" :rows="editFull ? 10 : (f.rows || 5)" :placeholder="f.placeholder" />
                    </template>
                    <template v-else-if="f.type === 'html'">
                      <Editor v-model="cfg[f.key]" :height="editFull ? 520 : (f.rows && f.rows <= 2 ? 220 : 340)" />
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
                    <template v-else-if="f.type === 'link'">
                      <el-select v-model="cfg[f.key]" filterable allow-create default-first-option placeholder="选择或输入页面地址" :style="f.width ? 'width:' + f.width + 'px' : ''">
                        <el-option v-for="opt in f.options" :key="opt" :label="opt" :value="opt" />
                      </el-select>
                      <div style="color:#999;font-size:12px;margin-top:4px">下拉为站内页面；也可自行输入外部链接（http:// 开头）</div>
                    </template>
                    <template v-else-if="f.type === 'list'">
                      <div v-for="(item, i) in cfg[f.key]" :key="i" class="card-row" style="margin-bottom:8px">
                        <template v-for="sf in f.fields">
                          <el-input v-if="sf.type !== 'html' && sf.type !== 'image'" :key="sf.key" v-model="item[sf.key]" :placeholder="sf.placeholder" :type="sf.type === 'textarea' ? 'textarea' : undefined" :rows="sf.rows || 3" :style="sf.width ? 'width:' + sf.width + 'px' : ''" />
                          <div v-else-if="sf.type === 'html'" :key="sf.key" style="width:100%">
                            <Editor v-model="item[sf.key]" :height="editFull ? 400 : 180" />
                          </div>
                          <div v-else-if="sf.type === 'image'" :key="sf.key" style="display:flex;align-items:center;gap:8px">
                            <el-upload class="avatar-uploader" :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="(res) => handleImageSuccess(res, item, sf.key)" accept="image/*">
                              <img v-if="item[sf.key]" :src="imgUrl(item[sf.key])" style="width:56px;height:56px;object-fit:cover;border-radius:50%" />
                              <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                            </el-upload>
                            <el-button v-if="item[sf.key]" type="text" icon="el-icon-delete" @click="item[sf.key] = ''">清除</el-button>
                          </div>
                        </template>
                        <el-button type="text" icon="el-icon-top" @click="moveListItem(f, i, -1)">上移</el-button>
                        <el-button type="text" icon="el-icon-bottom" @click="moveListItem(f, i, 1)">下移</el-button>
                        <el-button type="text" icon="el-icon-delete" @click="cfg[f.key].splice(i, 1)">删除</el-button>
                      </div>
                      <el-button type="primary" plain size="mini" @click="addListItem(f)">＋ 添加{{ f.itemLabel }}</el-button>
                    </template>
                  </el-form-item>
                </template>
                <el-form-item v-if="currentTpl && currentTpl.tip" label="说明">
                  <span style="color:#999;font-size:12px">{{ currentTpl.tip }}</span>
                </el-form-item>
              </template>
            </el-form>
          </template>
          <el-empty v-else description="请先在区块列表中选择要编辑的区块" />
        </div>

        <!-- 拖拽条：调整预览区宽度（360–900px，偏好记忆） -->
        <div v-if="!editFull" ref="resizer" class="sec-resizer" title="拖拽调整预览区宽度" @pointerdown="startResize"></div>

        <!-- 实时预览（同屏保留） -->
        <div v-if="!editFull" class="preview-area" :style="{ width: previewWidth + 'px' }">
          <div class="preview-bar">
            <span class="preview-title">前台实时预览 · {{ currentPage.name }}</span>
            <el-tag v-if="selectedId != null" size="mini" type="success">正在定位：{{ form.blockKey }}</el-tag>
            <el-tag v-else size="mini" type="info">未选中区块</el-tag>
          </div>
          <div class="preview-body">
            <iframe v-if="previewSrc" :key="previewTs" :src="previewSrc" class="preview-frame" @load="previewLoading = false" />
            <div v-if="previewLoading" class="preview-mask"><i class="el-icon-loading"></i> 预览加载中…</div>
          </div>
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
    <!-- L9 修复：destroy-on-close 关闭即销毁 iframe，避免预览页常驻占用 -->
    <el-dialog :title="'模板样式预览 · ' + tplPreviewName" :visible.sync="tplPreviewOpen" width="760px" append-to-body destroy-on-close>
      <div class="tpl-preview-wrap">
        <iframe v-if="tplPreviewSrc" :key="tplPreviewTs" :src="tplPreviewSrc" class="tpl-preview-frame"></iframe>
      </div>
    </el-dialog>

    <!-- 75：自定义页面管理弹窗（新增 + 列表操作） -->
    <el-dialog title="自定义页面" :visible.sync="pageMgrOpen" width="560px" append-to-body>
      <div style="margin-bottom:12px;color:#909399;font-size:13px">自定义页面 = 前台新分页，用区块模板搭建内容；前台访问地址：<code>page.html?key=页面标识</code>。会出现在前台「☰ 更多」菜单。</div>
      <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="openPageDlg">新增页面</el-button>
      <el-table :data="allPages" size="mini" style="margin-top:10px">
        <el-table-column label="页面名称" min-width="120">
          <template slot-scope="scope">
            {{ scope.row.pageName }}
            <el-tag v-if="!scope.row.custom" size="mini" type="info">内置</el-tag>
            <el-tag v-else-if="scope.row.status === '1'" size="mini" type="warning">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="页头标题" min-width="100">
          <template slot-scope="scope">{{ scope.row.heroTitle || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="230">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="editPageDlg(scope.row)">{{ scope.row.custom ? '编辑' : '页头设置' }}</el-button>
            <template v-if="scope.row.custom">
              <el-button size="mini" type="text" icon="el-icon-top" @click="moveCustomPage(scope.row, -1)">上移</el-button>
              <el-button size="mini" type="text" icon="el-icon-bottom" @click="moveCustomPage(scope.row, 1)">下移</el-button>
              <el-button size="mini" type="text" icon="el-icon-switch-button" @click="toggleCustomPage(scope.row)">{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" class="danger-text" @click="delCustomPage(scope.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer"><el-button @click="pageMgrOpen = false">关 闭</el-button></div>
    </el-dialog>

    <!-- 75：新增/编辑页面弹窗 -->
    <el-dialog :title="pageEditing ? '编辑页面' : '新增页面'" :visible.sync="pageDlgOpen" width="440px" append-to-body>
      <el-form :model="pageForm" label-width="90px" size="small">
        <el-form-item label="页面名称">
          <el-input v-model="pageForm.pageName" maxlength="50" placeholder="如：活动专题（后台 Tab 与前台更多菜单显示）" />
        </el-form-item>
        <el-form-item v-if="!pageEditing" label="页面标识">
          <el-input v-model="pageForm.pageKey" maxlength="50" placeholder="小写字母/数字/连字符，如 activity-2026" />
          <div style="color:#909399;font-size:12px;margin-top:4px">前台访问地址：page.html?key=此处填写的内容</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="pageForm.sort" :min="0" :max="999" size="small" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="pageForm.status">
            <el-radio label="0">启用（前台可见）</el-radio>
            <el-radio label="1">停用（前台隐藏）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="前台入口">
          <el-radio-group v-model="pageForm.menuPos">
            <el-radio label="more">「☰ 更多」菜单</el-radio>
            <el-radio label="nav">页面顶部导航栏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">页头（顶部大标题区）</el-divider>
        <el-form-item label="页头标题">
          <el-input v-model="pageForm.heroTitle" maxlength="100" placeholder="留空 = 不显示页头（页面直接从内容开始）" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="pageForm.heroSubtitle" type="textarea" :rows="2" maxlength="200" placeholder="页头下方的说明文字（可留空）" />
        </el-form-item>
        <el-form-item label="背景">
          <el-input v-model="pageForm.heroBg" maxlength="255" placeholder="图片 URL 或 CSS 色值（如 #0f1b2d）；留空 = 默认深蓝渐变" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="pageDlgOpen = false">取 消</el-button>
        <el-button type="primary" @click="savePage">确 定</el-button>
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
import { listBlock, addBlock, updateBlock, delBlock, moveBlock, listBlockHistory, rollbackBlock, copyBlock } from "@/api/system/cms"
import { listCmsPage, addCmsPage, updateCmsPage, delCmsPage } from "@/api/system/cms"
import { getConfigKey } from "@/api/system/config"
import { getToken } from "@/utils/auth"
import { BLOCK_TEMPLATES, templateOf, defaultCfgOf, sampleCfgOf } from "./blockTemplates"
// 68：html 字段排版工具栏——复用文章编辑的 Quill 组件，非程序员不再手写 HTML 标签
import Editor from "@/components/Editor"

export default {
  name: "CmsBlock",
  components: { Editor },
  // A：离开本路由时未保存修改拦截（点其他菜单/关闭页签）
  beforeRouteLeave(to, from, next) {
    if (this.dirty) {
      this.$modal.confirm('当前区块有未保存的修改，离开后将丢失。确定离开吗？', '未保存修改').then(() => next()).catch(() => next(false))
    } else {
      next()
    }
  },
  data() {
    return {
      builtinPages: [
        { key: 'home', name: '首页', file: 'home.html' },
        { key: 'about', name: '走进社区', file: 'about.html' },
        { key: 'join', name: '入驻招商', file: 'join.html' },
        { key: 'talent', name: '人才培养', file: 'talent.html' },
        { key: 'industry', name: '产业生态', file: 'industry.html' }
      ],
      customPages: [],          // 75：自定义页面（接口拉取，前台 page.html?key=xxx 动态渲染）
      pageDlgOpen: false,       // 75：页面管理弹窗
      pageForm: {},             // 75：新增/编辑页面表单
      pageEditing: false,
      pageMgrOpen: false,       // 75：自定义页面管理弹窗
      activePage: 'about',
      loading: false,
      blockList: [],
      selectedId: null,
      dirty: false,
      form: {},
      cfg: {},
      frontUrl: '',            // 前台地址（系统参数 site.front.url）；空/默认值 = 与后台同源，用相对路径
      // ① 编辑区宽度（可拖拽调宽，偏好存 localStorage；480–900 钳制）
      viewMode: 'pages',       // 改版：pages=页面选择 / blocks=区块列表 / edit=编辑（表单+预览双栏）
      previewWidth: (function () {
        // 预览区宽度（360–900px，偏好记忆；默认 520 贴近前台栏目页实际）
        try {
          const v = Number(localStorage.getItem('opc_block_preview_width'))
          return v >= 360 && v <= 900 ? v : 520
        } catch (e) { return 520 }
      })(),
      editFull: false,         // ② 放大编辑：隐藏预览，表单占满整行
      saving: false,           // B：保存防重复提交（Ctrl+S 连按）
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
    pages() {
      // 75：内置页 + 自定义页合并（自定义页前台走 page.html?key=）
      return [
        ...this.builtinPages,
        ...this.customPages.map(p => ({ key: p.pageKey, name: p.pageName, custom: true }))
      ]
    },
    /** 页面管理弹窗全量列表（内置页已注册进 cms_page，与自定义页统一展示；内置页仅页头可编辑） */
    allPages() {
      return this.customPages.map(p => ({ ...p, custom: !this.builtinPages.some(b => b.key === p.pageKey) }))
    },
    currentPage() {
      return this.pages.find(p => p.key === this.activePage) || this.pages[0]
    },
    contentBlocks() {
      return this.blockList.filter(b => b.template && b.template !== '')
    },
    slotBlocks() {
      // P1：显示全部固定文本槽（含已隐藏）——隐藏的槽位也要能选中恢复，防止"隐藏后永久消失只能改库"
      return this.blockList.filter(b => (!b.template || b.template === ''))
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
      // 自定义页无静态文件：预览走动态页 page.html?key=（同样支持预览高亮）
      if (this.currentPage.custom) {
        return this.frontBase + '/page.html?key=' + encodeURIComponent(this.activePage) + '&preview=1' + hl + '&t=' + this.previewTs
      }
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
    this.loadCustomPages()
    getConfigKey('site.front.url').then(res => {
      if (res && res.msg && res.msg !== 'http://localhost') this.frontUrl = res.msg
    }).catch(() => {})
    // B：Ctrl+S 保存（编辑长文时快速保存）
    window.addEventListener('keydown', this.onKeydown)
  },
  beforeDestroy() {
    window.removeEventListener('keydown', this.onKeydown)
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

    select(b) {
      // A：未保存修改拦截——确认后才切换/重置区块（同区块重复点击也会重置表单）
      if (this.dirty) {
        this.$modal.confirm('当前区块有未保存的修改，切换后将丢失。确定切换吗？', '未保存修改').then(() => {
          this.doSelect(b)
        }).catch(() => {})
        return
      }
      this.doSelect(b)
    },
    /** 改版：进入页面区块列表（dirty 拦截） */
    goBlocks(p) {
      if (this.dirty) {
        this.$modal.confirm('当前区块有未保存的修改，返回后将丢失。确定返回吗？', '未保存修改').then(() => {
          this.switchToBlocks(p)
        }).catch(() => {})
        return
      }
      this.switchToBlocks(p)
    },
    switchToBlocks(p) {
      this.selectedId = null
      this.activePage = p.key
      this.viewMode = 'blocks'
      this.getList(true)
    },
    /** 改版：返回页面选择（dirty 拦截） */
    goPages() {
      if (this.dirty) {
        this.$modal.confirm('当前区块有未保存的修改，返回后将丢失。确定返回吗？', '未保存修改').then(() => {
          this.viewMode = 'pages'
          this.selectedId = null
        }).catch(() => {})
        return
      }
      this.viewMode = 'pages'
      this.selectedId = null
    },
    /** 改版：编辑视图返回区块列表（goBlocks 内含 dirty 拦截） */
    goBlocksBack() {
      this.goBlocks(this.currentPage)
    },
    /** 改版：页面卡片区块数 */
    pageBlockCount(key) {
      return this.blockList.filter(b => b.pageKey === key && b.template && b.template !== '').length
    },
    doSelect(b) {
      this._suppressDirty = true
      this.selectedId = b.blockId
      this.form = b
      this.cfg = this.parseCfg(b.configJson, b.template)
      this.dirty = false
      this.viewMode = 'edit'   // 改版：选中区块 → 编辑视图（表单+预览双栏）
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
    /** C：列表项行内上移/下移（splice 触发 Vue 响应式 + dirty watch） */
    moveListItem(f, i, dir) {
      const arr = this.cfg[f.key]
      const j = i + dir
      if (j < 0 || j >= arr.length) return
      const t = arr[i]
      arr.splice(i, 1)
      arr.splice(j, 0, t)
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
    // 71：支持列表内子字段图片上传（target+key 传入时写子字段；顶层调用保持 cfg.image）
    handleImageSuccess(res, target, key) {
      if (res.code === 200) {
        if (key) { target[key] = res.fileName || res.url } else { this.cfg.image = res.fileName || res.url }
        this.$modal.msgSuccess("图片上传成功")
      }
    },
    handleSave() {
      // B：防重复提交（Ctrl+S 连按/双击保存）
      if (this.saving) return
      this.saving = true
      // 序列化配置（tags 的 tagsText 转数组）
      const cfg = JSON.parse(JSON.stringify(this.cfg))
      if (cfg.groups) cfg.groups.forEach(g => { g.tags = (g.tagsText || '').split('，').map(s => s.trim()).filter(Boolean); delete g.tagsText })
      this.form.configJson = JSON.stringify(cfg)
      updateBlock(this.form).then(() => {
        this.$modal.msgSuccess("已保存（历史已存档 v" + (this.form.version + 1) + "，预览已刷新）")
        this.dirty = false
        this.getList(false)
        this.reloadPreview()
      }).catch(() => {
        // B：保存失败明确提示（修改仍保留在表单，可重试）
        this.$modal.msgError("保存失败，请稍后重试（你的修改仍保留在表单中）")
      }).finally(() => { this.saving = false })
    },
    /** B：Ctrl+S 保存快捷键 */
    onKeydown(e) {
      if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) {
        e.preventDefault()
        if (this.selectedId != null) this.handleSave()
      }
    },
    /** 2：一键复制区块（克隆为新区块，可在新栏目页复用同款样式与内容） */
    handleCopy(b) {
      this.$modal.confirm('复制区块「' + (b.title || b.blockKey) + '」？复制后会在当前页面生成一个同名副本，可在新页面复用。').then(() => {
        return copyBlock(b.blockId)
      }).then(() => {
        this.$modal.msgSuccess("已复制（新副本在当前页面底部，可上移调整位置）")
        this.getList(true)
      }).catch(() => {})
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
    /** ① 拖拽调整编辑区宽度（min 480 / max 900，松手存 localStorage）
     *  Pointer Events + setPointerCapture：指针移入预览 iframe 后 mouseup/mousemove 被 iframe
     *  吞掉导致拖拽卡死（光标锁定、宽度无法缩小）——捕获后 pointer 事件强制派发回拖拽条，
     *  即使指针在 iframe 内/窗口外松手也必达；pointercancel 兜底清理，_resizing 防监听器叠加 */
    startResize(e) {
      if (!e.isPrimary || e.button !== 0) return
      if (this._dragCleanup) this._dragCleanup() // 自愈：上次拖拽 pointerup 被 iframe 吞掉 → 先清理遗留状态
      const target = this.$refs.resizer
      e.preventDefault()
      // 按下偏移：鼠标按在拖拽条上，预览宽度映射扣除按下点偏移才跟手
      const rect0 = this.$refs.layout.getBoundingClientRect()
      const offset = (rect0.right - e.clientX) - this.previewWidth
      let captured = true
      try { target.setPointerCapture(e.pointerId) } catch (err) { captured = false } // 合成事件等无活跃指针时回退 document 监听
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
      const onMove = (ev) => {
        const rect = this.$refs.layout.getBoundingClientRect()
        // 拖拽条在表单与预览之间：预览宽度 = 布局右缘 - 指针位置（镜像计算）
        this.previewWidth = Math.min(900, Math.max(360, rect.right - ev.clientX))
      }
      const onEnd = () => {
        document.body.style.cursor = ''
        document.body.style.userSelect = ''
        target.removeEventListener('pointermove', onMove)
        target.removeEventListener('pointerup', onEnd)
        target.removeEventListener('pointercancel', onEnd)
        document.removeEventListener('pointermove', onMove)
        document.removeEventListener('pointerup', onEnd)
        document.removeEventListener('pointercancel', onEnd)
        if (this._dragCleanup === onEnd) this._dragCleanup = null
        try { localStorage.setItem('opc_block_preview_width', String(this.previewWidth)) } catch (err) {}
      }
      this._dragCleanup = onEnd
      if (captured) {
        // 指针捕获：pointer 事件强制派发回拖拽条，iframe 内/窗口外松手必达
        target.addEventListener('pointermove', onMove)
        target.addEventListener('pointerup', onEnd)
        target.addEventListener('pointercancel', onEnd)
      } else {
        // 捕获失败兜底：document 级监听，指针移回文档任意处松手即结束
        document.addEventListener('pointermove', onMove)
        document.addEventListener('pointerup', onEnd)
        document.addEventListener('pointercancel', onEnd)
      }
    },
    /** ② 放大编辑切换：隐藏左右栏表单占满；退出时重建预览防陈旧 */
    toggleFull() {
      this.editFull = !this.editFull
      if (!this.editFull) this.reloadPreview()
    },
    /** 75：加载自定义页面（区块管理 Tab 动态化） */
    loadCustomPages() {
      listCmsPage().then(res => {
        this.customPages = res.data || []
      }).catch(() => {})
    },
    openPageDlg() {
      this.pageEditing = false
      this.pageForm = { pageKey: '', pageName: '', sort: 0, status: '0', menuPos: 'more', heroTitle: '', heroSubtitle: '', heroBg: '' }
      this.pageDlgOpen = true
    },
    editPageDlg(p) {
      this.pageEditing = true
      this.pageForm = { pageId: p.pageId, pageKey: p.pageKey, pageName: p.pageName, sort: p.sort, status: p.status, menuPos: p.menuPos || 'more', heroTitle: p.heroTitle || '', heroSubtitle: p.heroSubtitle || '', heroBg: p.heroBg || '' }
      this.pageDlgOpen = true
    },
    savePage() {
      const f = this.pageForm
      if (!f.pageName || !f.pageName.trim()) { this.$modal.msgWarning('请填写页面名称'); return }
      if (!this.pageEditing && !/^[a-z0-9-]{1,50}$/.test(f.pageKey || '')) {
        this.$modal.msgWarning('页面标识仅支持小写字母、数字、连字符（如 activity-2026）')
        return
      }
      const req = this.pageEditing ? updateCmsPage(f) : addCmsPage(f)
      req.then(() => {
        this.$modal.msgSuccess(this.pageEditing ? '页面已更新' : '页面已创建——前台访问地址：page.html?key=' + f.pageKey)
        this.pageDlgOpen = false
        this.loadCustomPages()
        if (!this.pageEditing) {
          // 创建后自动进入新页面区块列表，直接开始搭建（不再停留在选择页）
          this.switchToBlocks({ key: f.pageKey })
        }
      }).catch(() => {})
    },
    delCustomPage(p) {
      this.$modal.confirm('确认删除页面「' + p.pageName + '」吗？该页全部区块将一并删除，前台 page.html?key=' + p.pageKey + ' 将无法访问。').then(() => {
        return delCmsPage(p.pageId)
      }).then(() => {
        this.$modal.msgSuccess('页面已删除')
        if (this.activePage === p.pageKey) { this.selectedId = null; this.activePage = 'about' }
        this.loadCustomPages()
        this.getList(true)
      }).catch(() => {})
    },
    /** 75：关闭自定义页 Tab（确认后删除页面） */
    onTabRemove(key) {
      const p = this.customPages.find(x => x.pageKey === key)
      if (p) this.delCustomPage(p)
    },
    /** 75：启用/停用切换 */
    toggleCustomPage(p) {
      updateCmsPage({ pageId: p.pageId, status: p.status === '0' ? '1' : '0' }).then(() => {
        this.$modal.msgSuccess(p.status === '0' ? '已停用（前台不可访问）' : '已启用')
        this.loadCustomPages()
      }).catch(() => {})
    },
    moveCustomPage(p, dir) {
      const arr = this.customPages
      const idx = arr.findIndex(x => x.pageId === p.pageId)
      const o = arr[idx + dir]
      if (!o) return
      const tmp = p.sort
      updateCmsPage({ pageId: p.pageId, sort: o.sort }).then(() =>
        updateCmsPage({ pageId: o.pageId, sort: tmp })).then(() => {
        this.$modal.msgSuccess('已调整顺序')
        this.loadCustomPages()
      }).catch(() => {})
    },
    /** D：预览当前效果——把当前（含未保存）配置序列化，调前台单区块渲染器新标签打开 */
    previewCurrent() {
      const t = this.form.template
      if (!t) { this.$modal.msgWarning('固定文本槽无模板预览，保存后可在整页预览查看'); return }
      const cfg = JSON.parse(JSON.stringify(this.cfg))
      if (cfg.groups) cfg.groups.forEach(g => { g.tags = (g.tagsText || '').split('，').map(s => s.trim()).filter(Boolean); delete g.tagsText })
      const scene = this.currentTpl ? this.currentTpl.scene : ''
      const url = this.frontBase + '/block-preview.html?template=' + encodeURIComponent(t) +
        '&scene=' + encodeURIComponent(scene) +
        '&cfg=' + encodeURIComponent(JSON.stringify(cfg)) + '&t=' + Date.now()
      window.open(url, '_blank', 'noopener')
    },
    openFront() {
      window.open(this.frontBase + '/' + this.currentPage.file, '_blank', 'noopener')
    }
  }
}
</script>

<style scoped>
.page-picker { background:#fff; border:1px solid #ebeef5; border-radius:8px; padding:20px 24px; min-height:500px; }
.picker-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.picker-title { font-size:18px; font-weight:600; color:#303133; }
.page-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(240px, 1fr)); gap:16px; }
.page-card { border:1px solid #e4e7ed; border-radius:10px; padding:18px; cursor:pointer; transition:all .15s; position:relative; }
.page-card:hover { border-color:#409EFF; box-shadow:0 4px 14px rgba(64,158,255,.15); transform:translateY(-2px); }
.page-card-name { font-size:16px; font-weight:600; color:#303133; }
.page-card-sub { color:#909399; font-size:12px; margin-top:6px; }
.page-card-count { color:#606266; font-size:13px; margin-top:12px; }
.page-card-enter { position:absolute; right:14px; bottom:14px; color:#409EFF; font-size:12px; }

.blocks-view { background:#fff; border:1px solid #ebeef5; border-radius:8px; padding:16px 20px; min-height:500px; }
.blocks-head { display:flex; align-items:center; gap:12px; margin-bottom:14px; border-bottom:1px solid #f0f0f0; padding-bottom:12px; }
.blocks-title { font-size:16px; font-weight:600; }
.blocks-ops { margin-left:auto; display:flex; align-items:center; gap:8px; }
.blocks-body { max-width:900px; }
.blocks-group { margin-bottom:20px; }
.blocks-group-head { font-weight:600; font-size:14px; color:#606266; margin-bottom:10px; }
.blocks-count { color:#909399; font-weight:400; }
.block-card { border:1px solid #e4e7ed; border-radius:8px; padding:10px 14px; margin-bottom:8px; cursor:pointer; transition:all .15s; }
.block-card:hover { border-color:#409EFF; background:#f5f9ff; }
.block-card-slot { background:#fafbfc; }
.block-card-main { display:flex; align-items:center; gap:10px; }
.block-card-title { font-weight:600; font-size:14px; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.block-card-ops { margin-top:6px; display:flex; gap:2px; }
.block-card-sub { color:#909399; font-size:12px; margin-top:4px; }

.edit-layout { display:flex; flex-direction:column; height:calc(100vh - 170px); min-height:500px; background:#fff; border:1px solid #ebeef5; border-radius:8px; overflow:hidden; }
.edit-head { display:flex; align-items:center; gap:10px; padding:10px 14px; border-bottom:1px solid #ebeef5; flex-shrink:0; }
.edit-title { font-weight:600; font-size:15px; }
.edit-tag { flex-shrink:0; }
.edit-ops { margin-left:auto; display:flex; align-items:center; gap:4px; }
.edit-body { flex:1; display:flex; min-height:0; }
.edit-form-area { flex:1; min-width:0; overflow-y:auto; padding:14px 20px; }
.preview-area { flex-shrink:0; display:flex; flex-direction:column; border-left:1px solid #ebeef5; }
.preview-bar { display:flex; align-items:center; gap:8px; padding:8px 12px; border-bottom:1px solid #ebeef5; flex-shrink:0; }
.preview-title { font-weight:600; font-size:14px; }
.preview-body { flex:1; position:relative; background:#f2f3f5; }
.preview-frame { width:100%; height:100%; border:0; }
.preview-mask { position:absolute; inset:0; background:rgba(255,255,255,.65); display:flex; align-items:center; justify-content:center; color:#606266; font-size:14px; z-index:5; }
.sec-resizer { width:8px; flex-shrink:0; cursor:col-resize; background:transparent; border-radius:4px; touch-action:none; }
.sec-resizer:hover, .sec-resizer:active { background:#d9ecff; }
.tmpl-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.tmpl-card { border: 1px solid #e4e7ed; border-radius: 8px; padding: 14px; cursor: pointer; text-align: center; transition: all .15s; }
.tmpl-card:hover { border-color: #409EFF; box-shadow: 0 2px 10px rgba(64,158,255,.15); }
.tmpl-icon { font-size: 26px; }
.tmpl-name { font-weight: 600; margin: 6px 0 4px; }
.tmpl-desc { color: #909399; font-size: 12px; line-height: 1.5; }
.tmpl-actions { margin-top: 10px; display: flex; justify-content: center; gap: 6px; }
.tpl-preview-wrap { height: 520px; }
.tpl-preview-frame { width: 100%; height: 100%; border: 1px solid #ebeef5; border-radius: 6px; background: #f2f3f5; }
/* 卡片行：尊重 schema 内联宽度（icon 90 / 标题 150 等），Quill/textarea 独占一行换行展示 */
.card-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: flex-start; width: 100%; }
.card-row .el-input, .card-row .el-textarea { flex: 0 0 auto; }
.card-row > div { flex-basis: 100%; } /* html(Quill)/image 子字段容器独占一行 */
</style>
