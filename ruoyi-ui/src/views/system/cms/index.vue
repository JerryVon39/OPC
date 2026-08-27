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
            <!-- 6：批量移动栏目 -->
            <el-button type="primary" plain icon="el-icon-sort" size="mini" :disabled="multiple" @click="openMoveCategory" v-hasPermi="['system:cms:edit']">移动栏目</el-button>
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
              <!-- P2：预约发布（发布时间在未来）标「待发布」；1：定时下线（已过 offline_time）标「已定时下线」 -->
              <el-tag v-if="scope.row.status === '0' && scope.row.publishTime && new Date(String(scope.row.publishTime).replace(/-/g, '/')) > new Date()" type="warning" size="mini">待发布</el-tag>
              <el-tag v-else-if="scope.row.status === '0' && scope.row.offlineTime && new Date(String(scope.row.offlineTime).replace(/-/g, '/')) < new Date()" type="info" size="mini">已定时下线</el-tag>
              <el-tag v-else-if="scope.row.status === '0'" type="success" size="mini">已发布</el-tag>
              <el-tag v-else-if="scope.row.status === '1'" type="warning" size="mini">草稿</el-tag>
              <el-tag v-else type="info" size="mini">已下线</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" align="center" prop="publishTime" width="150" />
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="230">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:cms:edit']">修改</el-button>
              <!-- 2：一键复制（克隆为草稿） -->
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click="handleCopy(scope.row)" v-hasPermi="['system:cms:add']">复制</el-button>
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

    <!-- 6：批量移动栏目弹窗 -->
    <el-dialog title="移动到栏目" :visible.sync="moveCategoryOpen" width="400px" append-to-body>
      <el-select v-model="moveCategoryId" placeholder="请选择目标栏目" style="width:100%">
        <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
      </el-select>
      <div slot="footer">
        <el-button @click="moveCategoryOpen = false">取 消</el-button>
        <el-button type="primary" @click="confirmMoveCategory">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listArticle, delArticle, changeArticleStatus, publishArticle, updateArticle, listCategory, batchTop, batchStatus, batchSort, copyArticle, batchMoveCategory } from "@/api/system/cms"

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
      queryParams: { pageNum: 1, pageSize: 10, title: null, categoryId: null, categoryIds: null, status: null },
      moveCategoryOpen: false,  // 6：批量移动栏目弹窗
      moveCategoryId: null
    }
  },
  created() {
    // 支持从栏目管理页"发文章"跳转预选栏目（?categoryId=x），并联动列表过滤
    const preset = this.$route.query.categoryId
    if (preset) {
      this.queryParams.categoryId = Number(preset)
    }
    // 69：工作台「草稿」卡片直达（?status=1 = 草稿/未发布）
    const presetStatus = this.$route.query.status
    if (presetStatus) {
      this.queryParams.status = presetStatus
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
    /** 跳到独立编辑页（articleId=0 表示新增；带预选栏目 query） */
    goEdit(articleId) {
      const query = {}
      const preset = this.$route.query.categoryId
      if (articleId === 0 && preset) query.categoryId = preset
      this.$router.push({ path: '/content/article-edit/index/' + articleId, query })
    },
    loadCategoryOptions() {
      // P2：上限放宽（100 → 1000），栏目多于 100 个时不丢树节点
      listCategory({ pageNum: 1, pageSize: 1000 }).then(response => {
        const rows = response.rows || []
        this.categoryOptions = rows
        // 树按前台页面分组：📰 资讯动态（news.html 页）📄 政策赋能（policy.html 页）
        // 组节点带 categoryIds（组内栏目列表），点击按多栏目查询
        const isPolicy = r => r.frontPage === 'policy'
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
      return row.frontPage === 'policy'
    },
    /** 跳到栏目管理页（路由 = 内容运营目录路径 content + 菜单路径 category） */
    goCategory() {
      this.$router.push('/content/category')
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
      // 跳到独立编辑页（新增：articleId=0）
      // P1：预选当前选中栏目（树节点点击设置了 categoryId；组节点/全部不预选）
      const query = {}
      if (this.queryParams.categoryId) query.categoryId = this.queryParams.categoryId
      this.$router.push({ path: '/content/article-edit/index/0', query })
    },
    handleUpdate(row) {
      this.goEdit(row.articleId)
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
    // 批量下线（已发布 → 已下线；P1：选中含草稿时明示——草稿本就不展示，避免"被下线"误解）
    handleBatchOffline() {
      if (!this.ids.length) { this.$modal.msgWarning("请先勾选文章"); return }
      const draftCount = this.articleList.filter(a => this.ids.includes(a.articleId) && a.status === '1').length
      const tip = draftCount > 0 ? '（其中 ' + draftCount + ' 篇为草稿，草稿本就不在前台展示）' : ''
      this.$modal.confirm('确认将选中的 ' + this.ids.length + ' 篇文章下线？下线后前台不再展示。' + tip).then(() => {
        return batchStatus(this.ids, '2')
      }).then(() => {
        this.$modal.msgSuccess("已下线")
        this.getList()
      }).catch(() => {})
    },
    // 列表内改排序：即时保存（P2：300ms 防抖——连点数字框不再连发请求）
    handleSortChange(row) {
      clearTimeout(this._sortTimer)
      this._sortTimer = setTimeout(() => {
        batchSort([{ articleId: row.articleId, sort: row.sort }]).then(() => {
          this.$modal.msgSuccess("排序已保存")
        })
      }, 300)
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
    /** 2：一键复制——克隆为草稿，提示进入编辑 */
    handleCopy(row) {
      this.$modal.confirm('复制「' + (row.title || '').slice(0, 20) + '」为草稿副本？复制后可在列表找到并编辑。').then(() => {
        return copyArticle(row.articleId)
      }).then(res => {
        this.$modal.msgSuccess("已复制为草稿")
        this.getList()
      }).catch(() => {})
    },
    /** 6：批量移动栏目弹窗 */
    openMoveCategory() {
      if (!this.ids.length) { this.$modal.msgWarning("请先勾选文章"); return }
      this.moveCategoryOpen = true
    },
    confirmMoveCategory() {
      if (!this.moveCategoryId) { this.$modal.msgWarning("请选择目标栏目"); return }
      this.$modal.confirm('确认将选中的 ' + this.ids.length + ' 篇文章移动到「' + (this.categoryOptions.find(c => c.categoryId === this.moveCategoryId) || {}).categoryName + '」？').then(() => {
        return batchMoveCategory({ articleIds: this.ids, categoryId: this.moveCategoryId })
      }).then(() => {
        this.$modal.msgSuccess("已移动")
        this.moveCategoryOpen = false
        this.getList()
      }).catch(() => {})
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
