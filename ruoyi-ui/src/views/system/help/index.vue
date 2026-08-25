<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="help-header">
        <span>📖 使用帮助 · 数智游民创新工场后台</span>
      </div>
      <div class="help-body">
        <div v-for="g in groups" :key="g.title" class="help-group">
          <h3>{{ g.title }}</h3>
          <ol>
            <li v-for="(s, i) in g.steps" :key="i">{{ s }}</li>
          </ol>
        </div>
        <div class="help-tip">
          💡 误操作了怎么办？文章删除后进「运营辅助 → 文章回收站」还原；区块改坏了在区块管理点「历史版本」一键回滚；服务/成员同理。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "Help",
  data() {
    return {
      groups: [
        {
          title: '📰 发一篇新闻',
          steps: [
            '进入「内容运营 → 栏目管理」，确认新闻要放的栏目（如"新闻动态"），栏目不存在可先添加',
            '点该栏目行的「发文章」按钮，自动跳转到文章管理并预选栏目',
            '填写标题、作者、摘要，正文用富文本排版（加粗/插图/列表均可），上传封面',
            '状态选「已发布」并点确定；前台新闻动态页刷新即可看到'
          ]
        },
        {
          title: '🧩 改前台页面文案',
          steps: [
            '进入「内容运营 → 区块管理」，按页面 Tab 找到要改的区块（如首页"三大赋能"）',
            '展开区块，修改标题/内容，点「保存」',
            '打开前台页面刷新查看；内容留空 = 前台保持原样不覆盖',
            '改坏了：点「历史版本」→ 选旧版本 → 回滚，立即恢复'
          ]
        },
        {
          title: '🖼️ 换首页轮播图',
          steps: [
            '进入「内容运营 → 官网轮播」，新增/修改轮播项',
            '上传图片、填写标题与副标题，设置排序',
            '首页刷新即生效；不上传图片的轮播项前台自动跳过'
          ]
        },
        {
          title: '📄 传政策原文 PDF',
          steps: [
            '文章管理 → 修改政策文章 → 「附件」上传 PDF（≤20MB）',
            '前台政策详情页自动出现"下载政策原文"按钮'
          ]
        },
        {
          title: '📞 改联系电话/地址',
          steps: [
            '系统设置 → 参数设置，搜索 site_phone / site_address / site_wechat 修改',
            '前台所有页面的 footer 联系区与首页联系区刷新即生效'
          ]
        },
        {
          title: '👥 审批报名与入驻',
          steps: [
            '报名管理：查看报名列表，按服务/成员筛选',
            '入驻申请：点「通过」或「拒绝」，系统自动邮件通知申请人'
          ]
        }
      ]
    }
  }
}
</script>

<style scoped>
.help-header { font-size: 16px; font-weight: bold; }
.help-body { max-width: 860px; }
.help-group { margin-bottom: 22px; }
.help-group h3 { margin: 0 0 8px; color: #303133; }
.help-group ol { margin: 0; padding-left: 22px; line-height: 1.9; color: #606266; }
.help-tip {
  margin-top: 18px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #8a6d3b;
  font-size: 13px;
}
</style>
