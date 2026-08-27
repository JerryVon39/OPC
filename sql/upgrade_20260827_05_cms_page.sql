-- ============================================
-- 75：自定义前台页面——cms_page 注册表
-- 运营在「区块管理」新增前台分页：注册一行 + 前台 page.html?key=xxx 动态渲染该页区块
-- 幂等：CREATE TABLE IF NOT EXISTS
-- ============================================
CREATE TABLE IF NOT EXISTS `cms_page` (
  `page_id` bigint NOT NULL AUTO_INCREMENT COMMENT '页面ID',
  `page_key` varchar(50) NOT NULL COMMENT '页面标识（前台 page.html?key=xxx；小写字母数字连字符）',
  `page_name` varchar(50) NOT NULL COMMENT '页面名称（后台 Tab 与前台更多菜单显示）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`page_id`),
  UNIQUE KEY `uk_cms_page_key` (`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义前台页面注册表（区块管理新增分页）';
