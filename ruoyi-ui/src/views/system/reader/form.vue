<template>
  <div class="app-container">
    <el-card shadow="never" style="max-width: 600px; margin: 0 auto;">
      <div slot="header" class="form-title">📝 读者登记表</div>
      <el-form ref="elForm" :model="formData" :rules="rules" size="medium" label-width="100px">
        <el-form-item label="读者姓名" prop="readerName">
          <el-input v-model="formData.readerName" placeholder="请输入读者姓名" clearable>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号码" clearable>
          </el-input>
        </el-form-item>
        <el-form-item label="读者类型" prop="readerType">
          <el-select v-model="formData.readerType" placeholder="请选择读者类型" clearable style="width: 100%">
            <el-option v-for="dict in readerTypeOptions" :key="dict.dictValue" :label="dict.dictLabel"
              :value="dict.dictValue"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注"
            :autosize="{minRows: 4, maxRows: 4}"></el-input>
        </el-form-item>
        <el-form-item size="large">
          <el-button type="primary" :loading="submitting" @click="submitForm">提 交</el-button>
          <el-button @click="resetForm">重 置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { addReader } from "@/api/system/reader"
import { getDicts } from "@/api/system/dict/data"

export default {
  name: 'ReaderForm',
  data() {
    return {
      submitting: false,
      formData: {
        readerName: undefined,
        phone: undefined,
        readerType: undefined,
        remark: undefined,
      },
      rules: {
        readerName: [{ required: true, message: '请输入读者姓名', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号码', trigger: 'blur' }],
        readerType: [{ required: true, message: '请选择读者类型', trigger: 'change' }],
      },
      // 读者类型选项：从字典加载（原来是"选项一/选项二"占位）
      readerTypeOptions: [],
    }
  },
  created() {
    this.getDicts("reader_type").then(response => {
      this.readerTypeOptions = response.data;
    });
  },
  methods: {
    // 提交 = 调用后端 addReader 接口保存到数据库（原来只是 TODO）
    submitForm() {
      this.$refs['elForm'].validate(valid => {
        if (!valid) return
        this.submitting = true
        // 系统自动分配借书证号（登记时生成）
        this.formData.cardNo = 'JS' + Date.now().toString().slice(-8)
        addReader(this.formData).then(response => {
          this.$modal.msgSuccess("登记成功！借书证号：" + this.formData.cardNo)
          this.resetForm()
        }).finally(() => {
          this.submitting = false
        })
      })
    },
    resetForm() {
      this.$refs['elForm'].resetFields()
    },
  }
}
</script>

<style>
.form-title {
  font-weight: bold;
  font-size: 16px;
}
</style>
