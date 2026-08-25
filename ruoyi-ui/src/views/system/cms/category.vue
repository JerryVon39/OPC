<template>
  <div class="app-container">
    <!-- 顶部说明：面向非程序员，一句话讲清用法 -->
    <el-alert type="info" :closable="false" show-icon title="栏目即前台 Tab：在此新增/修改/排序栏目，文章管理页按栏目分类。有文章或子栏目的栏目不能直接删除，请先移走内容。" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd()" v-hasPermi="['system:cmsCategory:add']">添加主栏目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
      </el-col>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="treeList"
      row-key="categoryId"
      :default-expand-all="false"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="categoryName" label="栏目名称" width="300" />
      <el-table-column label="排序" align="center" width="120">
        <template slot-scope="scope">
          <el-input-number v-model="scope.row.sort" :min="0" :max="999" size="mini" controls-position="right" @change="handleSortChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" v-hasPermi="['system:cmsCategory:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" min-width="220">
        <template slot-scope="scope">
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
            <el-option v-for="c in flatOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
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

export default {
  name: "CmsCategory",
  data() {
    return {
      loading: true,
      refreshTable: true,
      treeList: [],
      flatOptions: [{ categoryId: 0, categoryName: "一级栏目" }],
      title: "",
      open: false,
      form: {},
      rules: {
        categoryName: [{ required: true, message: "栏目名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询栏目列表（平铺 → 组装树；同时生成弹窗的上级栏目下拉） */
    getList() {
      this.loading = true
      listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
        const rows = response.rows || []
        this.treeList = this.buildTree(rows, 0)
        // 弹窗上级栏目下拉：平铺树（前缀缩进展示层级，含"一级栏目"兜底项）
        this.flatOptions = [{ categoryId: 0, categoryName: "一级栏目" }]
        this.flattenTree(this.treeList, 0)
        this.refreshTable = false
        this.$nextTick(() => { this.refreshTable = true })
      }).finally(() => { this.loading = false })
    },
    /** 平铺栏目列表组装为树（深度限制 3 级，防过深操作复杂） */
    buildTree(rows, rootId, depth) {
      depth = depth || 1
      if (depth > 3) return []
      return rows
        .filter(r => r.parentId === rootId)
        .map(r => ({ ...r, children: this.buildTree(rows, r.categoryId, depth + 1) }))
    },
    /** 树展平为下拉选项（子级加缩进前缀） */
    flattenTree(nodes, level) {
      nodes.forEach(n => {
        this.flatOptions.push({ categoryId: n.categoryId, categoryName: (level > 0 ? '└ ' : '') + n.categoryName })
        if (n.children && n.children.length) this.flattenTree(n.children, level + 1)
      })
    },
    handleAdd(row) {
      this.reset()
      if (row && row.categoryId) {
        this.form.parentId = row.categoryId
        this.title = "在「" + row.categoryName + "」下添加子栏目"
      } else {
        this.title = "添加主栏目"
      }
      this.open = true
    },
    handleUpdate(row) {
      this.reset()
      getCategory(row.categoryId).then(response => {
        this.form = response.data
        this.title = "修改栏目"
        this.open = true
      })
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
            addCategory(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 列表内改排序：即时保存 */
    handleSortChange(row) {
      updateCategory({ categoryId: row.categoryId, sort: row.sort }).then(() => {
        this.$modal.msgSuccess("排序已保存")
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
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { categoryId: null, categoryName: null, parentId: 0, sort: 0, status: '0' }
      this.resetForm("form")
    }
  }
}
</script>
