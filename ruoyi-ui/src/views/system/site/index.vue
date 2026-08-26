<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" show-icon title="站点设置 = 前台导航/页脚/联系方式的统一管理入口（存储于系统参数 site.* 键）。保存后前台所有页面刷新即生效；内容留空则前台保持页面静态内容，不会覆盖。" />

    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">🧭 前台导航菜单（自上而下显示）</div>
      <el-form label-width="80px" size="small">
        <el-form-item v-for="(item, i) in navItems" :key="i" :label="'菜单 ' + (i + 1)">
          <div class="nav-row">
            <el-input v-model="item.name" placeholder="菜单名称（如：走进社区）" style="width:220px" maxlength="20" />
            <el-input v-model="item.link" placeholder="链接（如：about.html / home.html#contact）" style="width:320px" maxlength="100" />
            <el-button type="text" icon="el-icon-top" :disabled="i === 0" @click="moveNav(i, -1)">上移</el-button>
            <el-button type="text" icon="el-icon-bottom" :disabled="i === navItems.length - 1" @click="moveNav(i, 1)">下移</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-text" @click="navItems.splice(i, 1)">删</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain size="mini" @click="navItems.push({ name: '', link: '' })">＋ 添加菜单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">🦶 页脚三栏（支持 &lt;br&gt; 换行与简单加粗）</div>
      <el-form label-width="110px" size="small">
        <el-form-item label="关于我们">
          <el-input v-model="footer.about" type="textarea" :rows="3" placeholder="关于我们栏内容" />
        </el-form-item>
        <el-form-item label="联系我们">
          <el-input v-model="footer.contact" type="textarea" :rows="3" placeholder="联系我们栏内容" />
        </el-form-item>
        <el-form-item label="入驻与合作">
          <el-input v-model="footer.join" type="textarea" :rows="3" placeholder="入驻与合作栏内容" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">📞 联系方式（全站联系区/页脚联动）</div>
      <el-form label-width="110px" size="small">
        <el-form-item label="联系电话"><el-input v-model="contact.phone" placeholder="如：0763-3391888" style="width:300px" /></el-form-item>
        <el-form-item label="联系邮箱"><el-input v-model="contact.email" placeholder="如：opc@example.com" style="width:300px" /></el-form-item>
        <el-form-item label="社区地址"><el-input v-model="contact.address" placeholder="如：清远国家高新技术产业开发区天安智谷产业园 B6 栋" style="width:420px" /></el-form-item>
        <el-form-item label="公众号/视频号"><el-input v-model="contact.wechat" placeholder="格式：公众号名 ｜ 视频号名" style="width:300px" /></el-form-item>
      </el-form>
    </el-card>

    <div class="save-bar">
      <el-button type="primary" size="medium" @click="handleSave" v-hasPermi="['system:config:edit']">保存（保存后前台刷新即生效）</el-button>
    </div>
  </div>
</template>

<script>
import { updateConfig, getConfigKey, listConfig } from "@/api/system/config"

export default {
  name: "SiteSettings",
  data() {
    return {
      navItems: [],
      footer: { about: '', contact: '', join: '' },
      contact: { phone: '', email: '', address: '', wechat: '' }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      const keys = [
        ['site.nav', 'nav'], ['site.footer.about', 'fa'], ['site.footer.contact', 'fc'], ['site.footer.join', 'fj'],
        ['site_phone', 'phone'], ['site_email', 'email'], ['site_address', 'address'], ['site_wechat', 'wechat']
      ]
      Promise.all(keys.map(([key, field]) =>
        getConfigKey(key).then(res => {
          const v = res && res.msg ? res.msg : ''
          if (field === 'nav') {
            try { this.navItems = v ? JSON.parse(v) : [] } catch (e) { this.navItems = [] }
            if (!this.navItems.length) this.navItems.push({ name: '', link: '' })
          } else if (field === 'fa') this.footer.about = v
          else if (field === 'fc') this.footer.contact = v
          else if (field === 'fj') this.footer.join = v
          else this.contact[field] = v
        }).catch(() => {})
      ))
    },
    moveNav(i, dir) {
      const j = i + dir
      if (j < 0 || j >= this.navItems.length) return
      const tmp = this.navItems[i]
      this.navItems.splice(i, 1)
      this.navItems.splice(j, 0, tmp)
    },
    handleSave() {
      const navJson = JSON.stringify(this.navItems.filter(n => n.name && n.link))
      const values = {
        'site.nav': navJson, 'site.footer.about': this.footer.about, 'site.footer.contact': this.footer.contact,
        'site.footer.join': this.footer.join, 'site_phone': this.contact.phone, 'site_email': this.contact.email,
        'site_address': this.contact.address, 'site_wechat': this.contact.wechat
      }
      // RuoYi updateConfig 按 configId 更新：先查全部参数取各键 id
      listConfig({ pageNum: 1, pageSize: 100 }).then(res => {
        const rows = res.rows || []
        const saves = rows
          .filter(r => values[r.configKey] !== undefined)
          .map(r => updateConfig({ configId: r.configId, configName: r.configName, configKey: r.configKey, configValue: values[r.configKey] }))
        return Promise.all(saves)
      }).then(() => {
        this.$modal.msgSuccess("已保存（前台刷新即生效）")
      }).catch(() => {
        this.$modal.msgError("保存失败，请检查权限")
      })
    }
  }
}
</script>

<style scoped>
.section-card { margin-bottom: 16px; }
.card-header { font-weight: bold; color: #303133; }
.nav-row { display: flex; align-items: center; gap: 8px; }
.danger-text { color: #f56c6c; }
.save-bar { margin-top: 8px; }
</style>
