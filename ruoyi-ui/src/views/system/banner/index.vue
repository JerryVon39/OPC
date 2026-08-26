<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:banner:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:banner:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bannerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="轮播ID" align="center" prop="bannerId" width="80" />
      <el-table-column label="预览" align="center" width="150">
        <template slot-scope="scope">
          <div v-if="scope.row.image" style="width:130px;height:50px;border-radius:6px;overflow:hidden">
            <img :src="imgUrl(scope.row.image)" style="width:100%;height:100%;object-fit:cover" />
          </div>
          <div v-else style="width:130px;height:50px;border-radius:6px;background:linear-gradient(135deg,#24402f,#3d6a52);display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px">{{ scope.row.title }}</div>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" />
      <el-table-column label="副标题" align="center" prop="subtitle" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === '0'" type="success" size="mini">启用</el-tag>
          <el-tag v-else type="info" size="mini">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:banner:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:banner:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题（必填）" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" placeholder="请输入副标题" />
        </el-form-item>
        <el-form-item label="图片" prop="image">
          <el-upload
            class="avatar-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            accept="image/*"
            :before-upload="beforeImageUpload"
            :on-error="handleUploadError"
          >
            <img v-if="form.image" :src="imgUrl(form.image)" style="width:100%;max-height:120px;object-fit:cover;border-radius:6px" />
            <i v-else class="el-icon-plus avatar-uploader-icon" style="width:100%"></i>
          </el-upload>
          <el-button v-if="form.image" type="danger" plain size="mini" icon="el-icon-delete" style="margin-top:8px" @click="removeImage">移除图片</el-button>
          <div style="color:#999;font-size:12px">建议 1920×600 横向大图（效果最佳）；可不上传，留空则显示背景+文字</div>
        </el-form-item>
        <el-form-item label="图片适配" prop="imageFit">
          <el-radio-group v-model="form.imageFit">
            <el-radio label="cover">铺满裁切（推荐大图）</el-radio>
            <el-radio label="contain">完整显示</el-radio>
          </el-radio-group>
          <div style="color:#999;font-size:12px">图片比例与轮播区域不一致时：铺满裁切会裁剪边缘，完整显示会保留整图（两侧露出背景）</div>
        </el-form-item>
        <el-form-item label="背景样式" prop="bgColor">
          <el-select v-model="bgMode" style="width:200px" @change="onBgModeChange">
            <el-option label="默认深空渐变" value="default" />
            <el-option label="自定义纯色/渐变" value="custom" />
          </el-select>
          <div v-if="bgMode === 'custom'" style="margin-top:8px">
            <el-color-picker v-model="form.bgColor" style="vertical-align:middle" />
            <span style="color:#999;font-size:12px;margin:0 10px">纯色取色，或选渐变：</span>
            <el-select v-model="gradientPreset" size="small" style="width:180px" @change="onGradientChange" placeholder="渐变预设">
              <el-option v-for="g in gradientPresets" :key="g.value" :label="g.label" :value="g.value" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="文字颜色" prop="textColor">
          <el-color-picker v-model="form.textColor" />
          <span style="color:#999;font-size:12px;margin-left:10px">轮播文字/副标题颜色（默认白色）</span>
        </el-form-item>
        <el-form-item label="文字底色" prop="textBg">
          <el-select v-model="form.textBg" style="width:220px">
            <el-option label="无底色（直接显示在图上）" value="" />
            <el-option label="半透明黑 30%（通用）" value="rgba(0,0,0,0.30)" />
            <el-option label="半透明黑 60%（深色图更清晰）" value="rgba(0,0,0,0.60)" />
            <el-option label="不透明深色（最清晰）" value="rgba(11,26,46,0.92)" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转链接" prop="link">
          <el-input v-model="form.link" placeholder="如 /home.html 或空" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="99" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="效果预览">
          <div class="banner-live-preview" :style="{ background: form.bgColor || 'linear-gradient(135deg,#0b1a2e,#1d3f6e)' }">
            <img v-if="form.image" :src="imgUrl(form.image)" class="blp-img" :style="{ objectFit: form.imageFit || 'cover' }" alt="" />
            <div class="blp-text" :style="{ color: form.textColor || '#ffffff', background: form.textBg === undefined || form.textBg === null ? 'rgba(0,0,0,0.30)' : form.textBg }">
              <div class="blp-title">{{ form.title || '轮播标题' }}</div>
              <div class="blp-sub">{{ form.subtitle || '副标题' }}</div>
            </div>
          </div>
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
import { listBanner, getBanner, delBanner, addBanner, updateBanner } from "@/api/system/banner"
import { getToken } from "@/utils/auth"

export default {
  name: "Banner",
  computed: {
    // 上传鉴权头：computed 每次渲染动态取 token（模板作用域无法访问 import 的 getToken；
    // 登录过期重登后上传也始终使用最新令牌）
    uploadHeaders() {
      return { Authorization: "Bearer " + getToken() }
    }
  },
  data() {
    return {
      loading: true,
      showSearch: true,
      ids: [],
      multiple: true,
      total: 0,
      bannerList: [],
      title: "",
      open: false,
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload",
      queryParams: { pageNum: 1, pageSize: 10, title: null, status: null },
      form: {},
      bgMode: 'default',
      gradientPreset: '',
      gradientPresets: [
        { label: '深空科技蓝', value: 'linear-gradient(135deg,#0b1a2e,#1d3f6e)' },
        { label: '科技蓝亮', value: 'linear-gradient(135deg,#1d3f6e,#5b8df2)' },
        { label: '墨绿森林', value: 'linear-gradient(135deg,#16302a,#3d6a52)' },
        { label: '暖橙活力', value: 'linear-gradient(135deg,#7a3b1f,#c2571e)' },
        { label: '紫罗兰', value: 'linear-gradient(135deg,#2b1e5e,#6a4bc4)' },
        { label: '商务灰', value: 'linear-gradient(135deg,#23272e,#4a5568)' }
      ],
      rules: {
        title: [{ required: true, message: "标题不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    imgUrl(url) {
      if (!url) return ''
      if (url.startsWith('http') || url.startsWith(process.env.VUE_APP_BASE_API)) return url
      return process.env.VUE_APP_BASE_API + url
    },
    handleImageSuccess(res) {
      if (res.code === 200) {
        this.form.image = res.fileName || res.url
        this.$modal.msgSuccess("图片上传成功")
      } else {
        this.$modal.msgError("上传失败：" + (res.msg || ""))
      }
    },
    /** M4：图片上传前校验——仅图片、≤5MB（nginx 上限 20MB、后端 10MB） */
    beforeImageUpload(file) {
      if (file.type.indexOf('image/') !== 0) { this.$modal.msgError("仅支持图片文件"); return false }
      if (file.size > 5 * 1024 * 1024) { this.$modal.msgError("图片大小不能超过 5MB"); return false }
      return true
    },
    onBgModeChange(v) {
      if (v === 'default') this.form.bgColor = ''
    },
    onGradientChange(v) {
      this.form.bgColor = v
    },
    removeImage() {
      this.form.image = ''
    },
    handleUploadError(err) {
      // 401 = 登录令牌过期（el-upload 不走 axios 拦截器，过期不会自动跳登录）
      if (err && err.status === 401) {
        this.$modal.msgError("登录已过期，请重新登录后再上传")
      } else {
        this.$modal.msgError("上传失败，请检查网络或文件大小")
      }
    },
    getList() {
      this.loading = true
      listBanner(this.queryParams).then(response => {
        this.bannerList = response.rows
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
      this.ids = selection.map(item => item.bannerId)
      this.multiple = !selection.length
    },
    handleAdd() {
      this.bgMode = 'default'
      this.gradientPreset = ''
      this.reset()
      this.open = true
      this.title = "新增轮播图"
    },
    handleUpdate(row) {
      this.bgMode = row.bgColor ? 'custom' : 'default'
      this.gradientPreset = ''
      this.reset()
      getBanner(row.bannerId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改轮播图"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.bannerId != null) {
            updateBanner(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBanner(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const bannerIds = row.bannerId || this.ids
      this.$modal.confirm('确认删除该轮播图吗？').then(() => {
        return delBanner(bannerIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { bannerId: null, title: null, subtitle: null, image: null, link: null, sort: 0, status: '0' }
      this.resetForm("form")
    }
  }
}
</script>

<style scoped>
/* 弹窗内轮播效果实时预览 */
.banner-live-preview { position: relative; width: 100%; height: 120px; border-radius: 10px; overflow: hidden; display: flex; align-items: center; justify-content: center; }
.blp-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.blp-text { position: relative; text-align: center; padding: 12px 20px; background: rgba(0,0,0,0.25); border-radius: 10px; }
.blp-title { font-size: 18px; font-weight: bold; }
.blp-sub { font-size: 13px; opacity: 0.9; margin-top: 4px; }
</style>
