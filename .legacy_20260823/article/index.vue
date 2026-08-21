<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入文章标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="栏目" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择栏目" clearable>
          <el-option
            v-for="dict in dict.type.cms_category"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:article:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:article:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章标题" align="center" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span class="link-type" style="cursor:pointer" @click="handleView(scope.row)">{{ scope.row.title }}</span>
          <el-tag v-if="scope.row.isTop === '1'" type="danger" size="mini" style="margin-left:6px">置顶</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="栏目" align="center" prop="category" width="110">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.cms_category" :value="scope.row.category"/>
        </template>
      </el-table-column>
      <el-table-column label="作者" align="center" prop="author" width="150" />
      <el-table-column label="浏览量" align="center" prop="viewCount" width="90" />
      <el-table-column label="发布状态" align="center" width="120">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            active-text="发布"
            inactive-text="下架"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['system:article:publish']"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['system:article:query']"
          >查看</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:article:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:article:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改文章对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="880px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="文章标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入文章标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="栏目" prop="category">
              <el-select v-model="form.category" placeholder="请选择栏目">
                <el-option
                  v-for="dict in dict.type.cms_category"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置顶">
              <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="是" inactive-text="否" />
              <div style="color:#999;font-size:12px;line-height:1.4">置顶文章在前台列表优先展示</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图片" prop="cover">
              <image-upload v-model="form.cover" :limit="1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要" prop="summary">
              <el-input
                v-model="form.summary"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="请输入摘要（前台列表展示，300 字以内）"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文">
              <editor v-model="form.content" :min-height="280" />
              <div style="color:#999;font-size:12px;line-height:1.4">正文为富文本内容；前台以纯文本安全展示（防 XSS），建议首段写清核心信息</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 文章详情对话框 -->
    <el-dialog title="文章详情" :visible.sync="viewOpen" width="760px" append-to-body>
      <div v-if="viewData.articleId">
        <h2 style="margin:0 0 12px;font-size:20px;line-height:1.5">{{ viewData.title }}</h2>
        <div class="article-meta" style="font-size:12px;color:#8a8a8a;margin-bottom:10px;display:flex;gap:14px;flex-wrap:wrap">
          <span v-if="viewData.author">作者：{{ viewData.author }}</span>
          <span>发布时间：{{ viewData.createTime }}</span>
          <span>栏目：<dict-tag :options="dict.type.cms_category" :value="viewData.category"/></span>
          <span>浏览 {{ viewData.viewCount || 0 }} 次</span>
          <el-tag v-if="viewData.isTop === '1'" type="danger" size="mini">置顶</el-tag>
        </div>
        <div v-if="viewData.summary" style="font-size:13px;color:#666;background:#faf8f3;border-radius:8px;padding:10px 14px;margin-bottom:12px;line-height:1.8">
          <b>摘要：</b>{{ viewData.summary }}
        </div>
        <div v-html="renderBbcode(viewData.content)" style="font-size:14px;color:#3a3a3a;line-height:1.9"></div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listArticle, getArticle, delArticle, addArticle, updateArticle, changeArticleStatus } from "@/api/system/article"

export default {
  name: "Article",
  dicts: ['cms_category'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文章表格数据
      articleList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 详情对话框
      viewOpen: false,
      viewData: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        keyword: undefined,
        category: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
        category: [{ required: true, message: "请选择栏目", trigger: "change" }],
        author: [{ required: true, message: "作者不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询文章列表 */
    getList() {
      this.loading = true
      listArticle(this.queryParams).then(response => {
        this.articleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        articleId: undefined,
        category: undefined,
        title: undefined,
        summary: undefined,
        content: undefined,
        cover: undefined,
        author: undefined,
        isTop: "0",
        status: "0"
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.articleId)
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增文章"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const articleId = row.articleId || this.ids
      getArticle(articleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改文章"
      })
    },
    /** 查看详情 */
    handleView(row) {
      this.viewData = {}
      this.viewOpen = true
      getArticle(row.articleId).then(response => {
        this.viewData = response.data || {}
      })
    },
    /** 发布/下架开关 */
    handleStatusChange(row) {
      const text = row.status === '0' ? '发布' : '下架'
      this.$modal.confirm('确认"' + text + '"文章《' + row.title + '》吗？').then(() => {
        return changeArticleStatus(row.articleId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        // 取消/失败则回滚开关状态
        row.status = row.status === '0' ? '1' : '0'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.articleId != undefined) {
            updateArticle(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addArticle(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const articleIds = row.articleId || this.ids
      this.$modal.confirm('是否确认删除选中的文章数据项？').then(function() {
        return delArticle(articleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /**
     * BBCODE → HTML（查看详情用）
     * 安全策略：先整体 HTML 转义再替换标签（与后端 BbCodeUtil 一致，脚本天然失效，防 XSS）
     */
    renderBbcode(text) {
      if (!text) return ''
      let s = String(text)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
      s = s.replace(/\[b\]([\s\S]*?)\[\/b\]/g, '<b>$1</b>')
      s = s.replace(/\[quote\]([\s\S]*?)\[\/quote\]/g, '<blockquote style="margin:8px 0;padding:8px 12px;border-left:3px solid #d4a24e;background:#faf8f3;color:#666">$1</blockquote>')
      s = s.replace(/\[color=([a-zA-Z0-9#]{3,7})\]([\s\S]*?)\[\/color\]/g, '<span style="color:$1">$2</span>')
      s = s.replace(/\n/g, '<br/>')
      return s
    }
  }
}
</script>

<style scoped>
.link-type:hover {
  color: #d4a24e;
}
</style>
