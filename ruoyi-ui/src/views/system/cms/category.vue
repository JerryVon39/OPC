<template>
  <div class="app-container">
    <!-- 顶部说明：面向非程序员，一句话讲清用法 -->
    <el-alert type="info" :closable="false" show-icon title="栏目即前台 Tab：在此新增/修改/排序栏目，文章管理页按栏目分类。有文章或子栏目的栏目不能直接删除，请先移走内容。" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd()" v-hasPermi="['system:cmsCategory:add']">添加主栏目</el-button>
      </el-col>
      <el-col :span="4">
        <!-- C：栏目搜索（按名称过滤，保留命中行及其祖先） -->
        <el-input v-model="filterText" size="mini" prefix-icon="el-icon-search" placeholder="搜索栏目名称…" clearable style="width:200px" />
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
      </el-col>
    </el-row>

    <el-table
      ref="catTable"
      v-if="refreshTable"
      v-loading="loading"
      :data="filteredTree"
      row-key="categoryId"
      :default-expand-all="false"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="categoryName" label="栏目名称" width="260" />
      <!-- B：文章数（含子栏目累计） -->
      <el-table-column label="文章数" align="center" width="80">
        <template slot-scope="scope">
          <span :style="{ fontWeight: 600, color: scope.row.articleCount > 0 ? '#409EFF' : '#C0C4CC' }">{{ scope.row.articleCount }}</span>
        </template>
      </el-table-column>
      <!-- E：前台归属页面（政策类 → 政策赋能页，其余 → 资讯动态页；与文章列表口径一致） -->
      <el-table-column label="前台页面" align="center" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="isPolicyCat(scope.row) ? 'warning' : 'info'">{{ isPolicyCat(scope.row) ? '政策赋能页' : '资讯动态页' }}</el-tag>
        </template>
      </el-table-column>
      <!-- F：同级上移/下移 + 数字排序兜底 -->
      <el-table-column label="排序" align="center" width="140">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-top" @click="moveCategory(scope.row, -1)" :disabled="!canMove(scope.row, -1)" v-hasPermi="['system:cmsCategory:edit']">上移</el-button>
          <el-button size="mini" type="text" icon="el-icon-bottom" @click="moveCategory(scope.row, 1)" :disabled="!canMove(scope.row, 1)" v-hasPermi="['system:cmsCategory:edit']">下移</el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" v-hasPermi="['system:cmsCategory:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="320">
        <template slot-scope="scope">
          <!-- D：发文章（直达编辑页新增，预选栏目）/ 查看文章（列表筛选该栏目） -->
          <el-button size="mini" type="text" icon="el-icon-document-add" @click="goAddArticle(scope.row)" v-hasPermi="['system:cms:add']">发文章</el-button>
          <el-button size="mini" type="text" icon="el-icon-view" @click="goListArticle(scope.row)" v-hasPermi="['system:cms:list']">查看文章</el-button>
          <el-button size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)" v-hasPermi="['system:cmsCategory:add']">添加子栏目</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:cmsCategory:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:cmsCategory:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑栏目弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="480px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级栏目" prop="parentId">
          <el-select v-model="form.parentId" placeholder="留空为一级栏目" clearable style="width:100%">
            <!-- F6：编辑时自身及子孙禁选为上级（防树环） -->
            <el-option v-for="c in flatOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" :disabled="disabledCatIds.includes(c.categoryId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="栏目名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入栏目名称（必填，前台 Tab 显示）" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用（前台可见）</el-radio>
            <el-radio label="1">停用（前台隐藏）</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- I：前台归属页面可配置（替代名称前缀"政策"硬编码——新建栏目不再靠命名 hack 决定它在哪个页面） -->
        <el-form-item label="前台页面" prop="frontPage">
          <el-radio-group v-model="form.frontPage">
            <el-radio label="news">资讯动态页（新闻动态）</el-radio>
            <el-radio label="policy">政策赋能页</el-radio>
          </el-radio-group>
          <div class="field-tip" style="color:#909399;font-size:12px;line-height:1.6">决定该栏目文章出现在前台哪个页面；可随时修改。</div>
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
import { listCategory, getCategory, addCategory, updateCategory, delCategory } from "@/api/system/cms"
import { getCategoryCounts } from "@/api/system/cms"

export default {
  name: "CmsCategory",
  data() {
    return {
      loading: true,
      refreshTable: true,
      treeList: [],
      countMap: {},             // B：栏目→文章数映射（累加子孙前的基础数据）
      filterText: '',           // C：搜索关键词
      flatOptions: [{ categoryId: 0, categoryName: "一级栏目" }],
      disabledCatIds: [],  // F6：编辑时不可选为上级的栏目 id 集（自身+子孙）
      title: "",
      open: false,
      form: {},
      rules: {
        categoryName: [{ required: true, message: "栏目名称不能为空", trigger: "blur" }]
      }
    }
  },
  computed: {
    /** C：按名称过滤（保留命中行 + 祖先链，未命中子树折叠隐藏） */
    filteredTree() {
      const kw = this.filterText.trim()
      if (!kw) return this.treeList
      const hit = (n) => (n.categoryName || '').toLowerCase().includes(kw.toLowerCase())
      const filter = (nodes) => nodes
        .map(n => {
          const kids = n.children && n.children.length ? filter(n.children) : []
          if (hit(n) || kids.length) return { ...n, children: kids }
          return null
        })
        .filter(Boolean)
      return filter(this.treeList)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询栏目列表（平铺 → 组装树 + 文章数统计；同时生成弹窗的上级栏目下拉） */
    getList() {
      this.loading = true
      // B：栏目列表与文章数聚合并行拉取，都到位后再组装树（防 countMap 竞态）
      Promise.all([
        listCategory({ pageNum: 1, pageSize: 1000 }),
        getCategoryCounts().catch(() => ({ data: [] }))
      ]).then(([response, cntRes]) => {
        this.countMap = (cntRes.data || []).reduce((m, r) => { m[r.categoryId] = Number(r.articleCount || 0); return m }, {})
        const rows = response.rows || []
        this.treeList = this.buildTree(rows, 0)
        // 弹窗上级栏目下拉：平铺树（前缀缩进展示层级，含"一级栏目"兜底项）
        this.flatOptions = [{ categoryId: 0, categoryName: "一级栏目" }]
        this.flattenTree(this.treeList, 0)
        this.refreshTable = false
        this.$nextTick(() => { this.refreshTable = true })
      }).finally(() => { this.loading = false })
    },
    /** 平铺栏目组装为树（A：不再静默截断超深数据——旧数据不消失；节点带 depth 供新增时限制） */
    buildTree(rows, rootId, depth) {
      depth = depth || 1
      return rows
        .filter(r => r.parentId === rootId)
        .map(r => ({
          ...r,
          depth: depth,
          // B：文章数 = 自身 + 全部子孙（递归累加）
          articleCount: this.accumCount(rows, r.categoryId, depth),
          children: this.buildTree(rows, r.categoryId, depth + 1)
        }))
    },
    /** B：累计栏目（含子孙）文章数 */
    accumCount(rows, catId, depth) {
      let n = this.countMap[catId] || 0
      rows.filter(r => r.parentId === catId).forEach(c => { n += this.accumCount(rows, c.categoryId, depth + 1) })
      return n
    },
    /** 树展平为下拉选项（子级加缩进前缀） */
    flattenTree(nodes, level) {
      nodes.forEach(n => {
        this.flatOptions.push({ categoryId: n.categoryId, categoryName: (level > 0 ? '└ ' : '') + n.categoryName })
        if (n.children && n.children.length) this.flattenTree(n.children, level + 1)
      })
    },
    /** E：前台归属页面（与文章列表 isPolicyCat 口径一致） */
    isPolicyCat(row) {
      return row.frontPage === 'policy'
    },
    /** F：同级上移/下移（交换 sort 后重新拉取） */
    canMove(row, dir) {
      const sibs = this.siblingsOf(row)
      const idx = sibs.findIndex(s => s.categoryId === row.categoryId)
      return dir === -1 ? idx > 0 : idx >= 0 && idx < sibs.length - 1
    },
    siblingsOf(row) {
      const out = []
      const walk = (nodes, parentId) => {
        nodes.forEach(n => {
          if (n.parentId === parentId) out.push(n)
          if (n.children && n.children.length) walk(n.children, n.categoryId)
        })
      }
      walk(this.treeList, row.parentId || 0)
      return out.sort((a, b) => (a.sort || 0) - (b.sort || 0) || a.categoryId - b.categoryId)
    },
    moveCategory(row, dir) {
      const sibs = this.siblingsOf(row)
      const idx = sibs.findIndex(s => s.categoryId === row.categoryId)
      const other = sibs[idx + dir]
      if (!other) return
      const tmp = row.sort
      const updates = []
      updates.push(updateCategory({ categoryId: row.categoryId, sort: other.sort }))
      updates.push(updateCategory({ categoryId: other.categoryId, sort: tmp }))
      Promise.all(updates).then(() => {
        this.$modal.msgSuccess("已调整顺序")
        this.getList()
      }).catch(() => {})
    },
    handleAdd(row) {
      this.reset()
      this.disabledCatIds = [] // 新增：上级可选任意栏目
      if (row && row.categoryId) {
        // A：超过 3 级阻止（树最深支持 3 级，避免超深栏目在前台/后台难管理）
        if (row.depth >= 3) {
          this.$modal.msgWarning("栏目最多支持 3 级，请在更上层栏目下添加")
          return
        }
        this.form.parentId = row.categoryId
        this.title = "在「" + row.categoryName + "」下添加子栏目"
      } else {
        this.title = "添加主栏目"
      }
      this.open = true
    },
    handleUpdate(row) {
      this.reset()
      // F6：编辑时自身及子孙不可选为上级（防树环）
      this.disabledCatIds = this.collectSelfAndDesc(row.categoryId)
      getCategory(row.categoryId).then(response => {
        this.form = response.data
        this.title = "修改栏目"
        this.open = true
      })
    },
    /** F6：收集自身及全部子孙 id */
    collectSelfAndDesc(id) {
      const ids = new Set([id])
      let root = null
      const find = (nodes) => {
        for (const n of nodes) {
          if (n.categoryId === id) { root = n; return }
          if (n.children && n.children.length) find(n.children)
        }
      }
      find(this.treeList)
      if (root) {
        const addSub = (n) => { if (n.children) n.children.forEach(c => { ids.add(c.categoryId); addSub(c) }) }
        addSub(root)
      }
      return [...ids]
    },
    /** 递归查找行（parentId+名称匹配新增栏目） */
    findRow(nodes, match) {
      for (const n of nodes) {
        if (match(n)) return n
        if (n.children && n.children.length) { const r = this.findRow(n.children, match); if (r) return r }
      }
      return null
    },
    /** 树定位到指定栏目（展开祖先链 + 高亮当前行） */
    revealCategory(categoryId) {
      const table = this.$refs.catTable
      if (!table) return
      const parentOf = {}
      const walk = (nodes) => nodes.forEach(n => { if (n.children) n.children.forEach(c => { parentOf[c.categoryId] = n.categoryId; walk([c]) }) })
      walk(this.treeList)
      const chain = []
      let cur = categoryId
      while (parentOf[cur]) { chain.push(parentOf[cur]); cur = parentOf[cur] }
      chain.forEach(k => { const node = table.store.nodesMap[k]; if (node) node.expanded = true })
      const node = table.store.nodesMap[categoryId]
      if (node) table.setCurrentRow(node.data)
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.categoryId != null) {
            updateCategory(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            const parentId = this.form.parentId
            const catName = this.form.categoryName
            addCategory(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList().then(() => {
                // 定位新增栏目（展开祖先链 + 高亮），不再"新增完找不到"
                const row = this.findRow(this.treeList, r => r.parentId === parentId && r.categoryName === catName)
                if (row) this.$nextTick(() => this.revealCategory(row.categoryId))
              })
            })
          }
        }
      })
    },
    /** 列表内切换启用/停用 */
    handleStatusChange(row) {
      updateCategory({ categoryId: row.categoryId, status: row.status }).then(() => {
        this.$modal.msgSuccess(row.status === '0' ? "栏目已启用" : "栏目已停用（前台隐藏）")
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除栏目「' + row.categoryName + '」吗？栏目下有文章或子栏目时系统会拒绝删除。').then(() => {
        return delCategory(row.categoryId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** D：发文章——直达编辑页新增（预选该栏目），不再先跳列表再点新增 */
    goAddArticle(row) {
      this.$router.push('/content/article-edit/index/0?categoryId=' + row.categoryId)
    },
    /** D：查看文章——文章列表筛选该栏目 */
    goListArticle(row) {
      this.$router.push('/content/article?categoryId=' + row.categoryId)
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { categoryId: null, categoryName: null, parentId: 0, sort: 0, status: '0', frontPage: 'news' }
      this.resetForm("form")
    }
  }
}
</script>
