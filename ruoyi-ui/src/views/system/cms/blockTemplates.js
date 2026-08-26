/**
 * 内容区块模板注册表（Schema 驱动：后台表单按 schema 自动生成，新增模板零前端代码）
 * scene: 'home' 首页整屏型（home-mod 样式）| 'page' 栏目页文档型（pblock 样式）| 'both' 通用
 * schema 字段类型：text/textarea/html/number/radio/switch/image/list（list 含子字段 fields）
 */
export const BLOCK_TEMPLATES = [
  {
    value: 'hero', name: '首屏（轮播+文案）', icon: '🎠', scene: 'home',
    desc: '首页首屏：标题/副标题/正文（轮播走官网轮播管理）',
    schema: [
      { key: 'title', label: '主标题', type: 'text', maxlength: 60, placeholder: '留空 = 使用默认首屏标题' },
      { key: 'subtitle', label: '副标题', type: 'text', maxlength: 100, placeholder: '留空 = 使用默认副标题' },
      { key: 'content', label: '正文', type: 'textarea', rows: 4, placeholder: '留空 = 使用默认文案' }
    ],
    tip: '首屏轮播图请到「官网轮播」维护；文案留空则前台用默认文案。'
  },
  {
    value: 'news', name: '新闻动态', icon: '📰', scene: 'home',
    desc: '自动拉取最新新闻列表，可设条数（首页）',
    schema: [
      { key: 'count', label: '显示条数', type: 'number', min: 1, max: 12 }
    ]
  },
  {
    value: 'contact', name: '联系区', icon: '📮', scene: 'home',
    desc: '时间线 + 联系方式（联系方式自动读站点配置）',
    schema: [
      { key: 'items', label: '时间线条目', type: 'list', itemLabel: '条目', fields: [
        { key: 'date', label: '日期', type: 'text', width: 130, placeholder: '日期' },
        { key: 'title', label: '标题', type: 'text', width: 150, placeholder: '标题' },
        { key: 'desc', label: '描述', type: 'text', placeholder: '描述' }
      ]}
    ],
    tip: 'contact 模板的联系方式自动读取站点配置（系统设置 → 参数设置）'
  },
  {
    value: 'text', name: '文本段落', icon: '📝', scene: 'both',
    desc: '标题 + 长文段落（支持简单排版）',
    schema: [
      { key: 'subtitle', label: '副标题', type: 'text', placeholder: '可选副标题' },
      { key: 'text', label: '正文', type: 'html', rows: 6, placeholder: '正文（支持加粗/列表等简单排版）' }
    ]
  },
  {
    value: 'feature', name: '图文并排', icon: '🖼️', scene: 'page',
    desc: '配图 + 标题 + 正文 + 按钮，可左右换向',
    schema: [
      { key: 'image', label: '配图', type: 'image' },
      { key: 'text', label: '正文', type: 'html', rows: 4, placeholder: '正文' },
      { key: 'btnText', label: '按钮文字', type: 'text', width: 200, placeholder: '如：了解更多' },
      { key: 'btnLink', label: '按钮链接', type: 'text', width: 300, placeholder: '如：join.html' },
      { key: 'reverse', label: '图文换向', type: 'switch', activeValue: '1', inactiveValue: '0', activeText: '图在右', inactiveText: '图在左' }
    ]
  },
  {
    value: 'cards', name: '图标卡片', icon: '🃏', scene: 'both',
    desc: '2/3 列图标卡片（价值点/课程/企业）',
    schema: [
      { key: 'cols', label: '每行列数', type: 'radio', options: [2, 3] },
      { key: 'subtitle', label: '引导语', type: 'text', placeholder: '可选：卡片上方的引导说明' },
      { key: 'cards', label: '卡片', type: 'list', itemLabel: '卡片', fields: [
        { key: 'icon', label: '图标', type: 'text', width: 90, placeholder: '图标 emoji' },
        { key: 'title', label: '卡片标题', type: 'text', width: 150, placeholder: '卡片标题' },
        { key: 'text', label: '卡片正文', type: 'html', rows: 2, placeholder: '卡片正文（支持简单排版）' }
      ]}
    ]
  },
  {
    value: 'steps', name: '步骤清单', icon: '🔢', scene: 'page',
    desc: '①②③ 编号步骤（入驻流程/申报流程）',
    schema: [
      { key: 'steps', label: '步骤', type: 'list', itemLabel: '步骤', fields: [
        { key: 'title', label: '步骤标题', type: 'text', width: 200, placeholder: '步骤标题' },
        { key: 'desc', label: '步骤说明', type: 'text', placeholder: '步骤说明' }
      ]}
    ]
  },
  {
    value: 'list', name: '条目清单', icon: '📋', scene: 'page',
    desc: '圆点列表，每条小标题+描述（政策/权益）',
    schema: [
      { key: 'items', label: '条目', type: 'list', itemLabel: '条目', fields: [
        { key: 'title', label: '小标题', type: 'text', width: 200, placeholder: '小标题（如：咨询电话）' },
        { key: 'desc', label: '描述', type: 'text', placeholder: '描述' }
      ]}
    ]
  },
  {
    value: 'tags', name: '标签墙', icon: '🏷️', scene: 'both',
    desc: '分组标签列表（机构/合作方）',
    schema: [
      { key: 'groups', label: '分组', type: 'list', itemLabel: '分组', fields: [
        { key: 'title', label: '分组名', type: 'text', width: 220, placeholder: '分组名（如：首批入驻企业）' },
        { key: 'tagsText', label: '标签', type: 'textarea', rows: 2, placeholder: '标签，用中文逗号分隔' }
      ]}
    ]
  },
  {
    value: 'timeline', name: '时间线', icon: '📅', scene: 'both',
    desc: '时间节点列表（发展历程）',
    schema: [
      { key: 'items', label: '节点', type: 'list', itemLabel: '节点', fields: [
        { key: 'date', label: '日期', type: 'text', width: 130, placeholder: '日期' },
        { key: 'title', label: '标题', type: 'text', width: 150, placeholder: '标题' },
        { key: 'desc', label: '描述', type: 'text', placeholder: '描述' }
      ]}
    ]
  },
  {
    value: 'stats', name: '数据亮点', icon: '📊', scene: 'page',
    desc: '2-4 个数字+说明（实力展示）',
    schema: [
      { key: 'items', label: '数据', type: 'list', itemLabel: '数据', fields: [
        { key: 'value', label: '数值', type: 'text', width: 130, placeholder: '数值（如：21 天）' },
        { key: 'label', label: '说明', type: 'text', width: 220, placeholder: '说明（如：从签约到揭牌）' }
      ]},
      { key: 'text', label: '补充说明', type: 'html', rows: 2, placeholder: '可选：数据下方补充文字' }
    ]
  },
  {
    value: 'quote', name: '金句引用', icon: '💬', scene: 'page',
    desc: '大字号引语 + 出处',
    schema: [
      { key: 'text', label: '金句', type: 'textarea', rows: 3, placeholder: '引语内容' },
      { key: 'author', label: '出处', type: 'text', width: 260, placeholder: '如：社区运营理念' }
    ]
  },
  {
    value: 'cta', name: 'CTA 横幅', icon: '🎯', scene: 'both',
    desc: '大按钮引导横幅（页尾引导）',
    schema: [
      { key: 'title', label: '主标题', type: 'text', placeholder: '留空则用区块名称' },
      { key: 'text', label: '正文', type: 'textarea', rows: 2, placeholder: '引导文案' },
      { key: 'btnText', label: '按钮文字', type: 'text', width: 200, placeholder: '如：立即入驻' },
      { key: 'btnLink', label: '按钮链接', type: 'text', width: 300, placeholder: '如：join.html' }
    ]
  },
  {
    value: 'form', name: '申请表单', icon: '📮', scene: 'page',
    desc: '入驻申请表单（join 页专用，字段固定）',
    schema: [],
    tip: '入驻申请表单（字段固定：名称/联系人/邮箱/说明），提交后由运营团队处理。可在此调整区块名称与显示状态。'
  }
]

/** 按模板值取定义 */
export function templateOf(value) {
  return BLOCK_TEMPLATES.find(t => t.value === value)
}

/** 由 schema 推导默认配置（list 字段给一个空项） */
export function defaultCfgOf(template) {
  const t = templateOf(template)
  const cfg = {}
  ;(t ? t.schema : []).forEach(f => {
    if (f.type === 'list') {
      const item = {}
      f.fields.forEach(sf => { item[sf.key] = '' })
      cfg[f.key] = [item]
    } else if (f.type === 'number') {
      cfg[f.key] = f.min != null ? f.min : 0
    } else if (f.type === 'radio') {
      cfg[f.key] = f.options[0]
    } else if (f.type === 'switch') {
      cfg[f.key] = f.inactiveValue
    } else {
      cfg[f.key] = ''
    }
  })
  return cfg
}
