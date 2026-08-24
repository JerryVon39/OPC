<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- Tab1：SMTP 配置 -->
      <el-tab-pane label="SMTP 配置" name="config">
        <el-card v-loading="configLoading" shadow="never">
          <el-form ref="configForm" :model="config" label-width="120px" style="max-width:560px">
            <el-form-item label="邮件通知开关">
              <el-radio-group v-model="config.enabled">
                <el-radio label="1">开启</el-radio>
                <el-radio label="0">关闭（业务照常，不发邮件）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="SMTP 主机">
              <el-input v-model="config.host" placeholder="smtp.qq.com" />
            </el-form-item>
            <el-form-item label="SMTP 端口">
              <el-input-number v-model="config.port" :min="1" :max="65535" controls-position="right" />
            </el-form-item>
            <el-form-item label="发件邮箱">
              <el-input v-model="config.username" placeholder="用于发信的通知邮箱" />
            </el-form-item>
            <el-form-item label="SMTP 授权码">
              <el-input v-model="config.authCode" type="password" show-password
                :placeholder="config.authCodeConfigured ? '已配置（留空表示不修改）' : 'QQ 邮箱 → 设置 → 账户 → 开启 SMTP 服务获取'" />
              <div v-if="config.authCodeConfigured" style="font-size:12px;color:#909399;line-height:1.6;margin-top:4px">
                ✓ 已配置授权码
                <el-tag v-if="config.authCodeEncrypted" type="success" size="mini" style="margin-left:6px">已加密存储</el-tag>
                <el-tag v-else type="warning" size="mini" style="margin-left:6px">明文存储（建议设置 MAIL_SECRET_KEY 环境变量后重新保存）</el-tag>
              </div>
            </el-form-item>
            <el-form-item label="发件人昵称">
              <el-input v-model="config.fromName" placeholder="选填，如：数智游民创新工场" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveConfig">保存配置（即时生效，无需重启）</el-button>
            </el-form-item>
          </el-form>
          <el-divider content-position="left">测试发送</el-divider>
          <div style="max-width:560px">
            <el-input v-model="testTo" placeholder="输入测试收件邮箱，验证当前配置能否发信" style="width:360px" />
            <el-button type="success" :loading="testing" @click="testSend" style="margin-left:10px">发送测试邮件</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab2：模板管理 -->
      <el-tab-pane label="邮件模板" name="template">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="getTemplates">刷新</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getTemplates"></right-toolbar>
        </el-row>
        <el-table v-loading="tplLoading" :data="templateList">
          <el-table-column label="模板编码" align="center" prop="code" width="180" />
          <el-table-column label="模板名称" align="center" prop="name" width="160" />
          <el-table-column label="邮件主题" align="center" prop="subject" :show-overflow-tooltip="true" />
          <el-table-column label="状态" align="center" width="90">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status === '1'" type="success" size="mini">启用</el-tag>
              <el-tag v-else type="info" size="mini">停用（回退默认）</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="占位符说明" align="center" prop="remark" :show-overflow-tooltip="true" />
          <el-table-column label="最后修改" align="center" prop="updateTime" width="160" />
          <el-table-column label="操作" align="center" width="120">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-edit" @click="openTemplate(scope.row)"
                v-hasPermi="['system:mail:template']">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 模板编辑对话框 -->
        <el-dialog :title="tplForm.code + ' · ' + tplForm.name" :visible.sync="tplOpen" width="720px" append-to-body>
          <el-form ref="tplForm" :model="tplForm" label-width="80px">
            <el-form-item label="模板名称">
              <el-input v-model="tplForm.name" placeholder="模板名称" />
            </el-form-item>
            <el-form-item label="邮件主题">
              <el-input v-model="tplForm.subject" placeholder="支持 {占位符}" />
            </el-form-item>
            <el-form-item label="邮件正文">
              <el-input v-model="tplForm.content" type="textarea" :rows="12" placeholder="HTML 正文，支持 {占位符}，如 {readerName} {cardNo} {bookName} {dueDate} {days} {code} {minutes} {applyName}" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="tplForm.status">
                <el-radio label="1">启用</el-radio>
                <el-radio label="0">停用（发送时回退内置默认模板）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="占位符说明">
              <el-input v-model="tplForm.remark" type="textarea" :rows="2" placeholder="记录本模板可用占位符，如：{readerName} 姓名、{cardNo} 成员编号" />
            </el-form-item>
          </el-form>
          <div slot="footer" class="dialog-footer">
            <el-button type="primary" :loading="savingTpl" @click="saveTemplate">保存（即时生效）</el-button>
            <el-button @click="tplOpen = false">取 消</el-button>
          </div>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getConfig, saveConfig, testSend } from "@/api/system/mail";
import { listTemplate, getTemplate, updateTemplate } from "@/api/system/mailTemplate";

export default {
  name: "Mail",
  data() {
    return {
      activeTab: "config",
      showSearch: true,
      configLoading: false,
      saving: false,
      testing: false,
      testTo: "",
      config: { enabled: "1", host: "smtp.qq.com", port: 465, username: "", authCode: "", fromName: "", authCodeConfigured: false, authCodeEncrypted: false },
      tplLoading: false,
      templateList: [],
      tplOpen: false,
      savingTpl: false,
      tplForm: { code: "", name: "", subject: "", content: "", status: "1", remark: "" }
    };
  },
  created() {
    this.getConfig();
    this.getTemplates();
  },
  methods: {
    getConfig() {
      this.configLoading = true;
      getConfig().then(res => {
        this.config = res.data || this.config;
        this.configLoading = false;
      }).catch(() => { this.configLoading = false; });
    },
    saveConfig() {
      this.saving = true;
      saveConfig(this.config).then(() => {
        this.$modal.msgSuccess("配置已保存，即时生效");
        this.getConfig();
      }).finally(() => { this.saving = false; });
    },
    testSend() {
      if (!this.testTo || !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(this.testTo)) {
        this.$modal.msgError("请输入有效的测试邮箱");
        return;
      }
      this.testing = true;
      testSend(this.testTo).then(res => {
        this.$modal.msgSuccess(res.msg || "测试邮件已发送");
      }).catch(err => {
        this.$modal.msgError(err.msg || "发送失败，请检查配置");
      }).finally(() => { this.testing = false; });
    },
    getTemplates() {
      this.tplLoading = true;
      listTemplate().then(res => {
        this.templateList = res.data || [];
        this.tplLoading = false;
      }).catch(() => { this.tplLoading = false; });
    },
    openTemplate(row) {
      getTemplate(row.code).then(res => {
        this.tplForm = res.data || row;
        this.tplOpen = true;
      });
    },
    saveTemplate() {
      this.savingTpl = true;
      updateTemplate(this.tplForm).then(() => {
        this.$modal.msgSuccess("模板已保存，即时生效");
        this.tplOpen = false;
        this.getTemplates();
      }).finally(() => { this.savingTpl = false; });
    }
  }
};
</script>
