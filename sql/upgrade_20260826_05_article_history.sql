-- ============================================
-- 升级脚本：文章版本历史 v20260826（批次 A：数据安全底线）
-- 变更：
--   1. cms_article 加 version 列（当前版本号，默认 1）
--   2. 新建 cms_article_history 表（保存前写当前版，20 版上限，对齐 cms_block_history 模式）
-- 幂等：information_schema 补列 + CREATE TABLE IF NOT EXISTS
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p ry-vue < sql/upgrade_20260826_article_history.sql
-- ============================================

USE ry-vue;

-- 1. cms_article 补 version 列
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_article' AND COLUMN_NAME = 'version');
SET @s = IF(@c = 0, 'ALTER TABLE cms_article ADD COLUMN version int NOT NULL DEFAULT 1 COMMENT ''当前版本号''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2. 历史表
CREATE TABLE IF NOT EXISTS cms_article_history (
  history_id  bigint       NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  article_id  bigint       NOT NULL COMMENT '文章ID',
  version     int          NOT NULL COMMENT '版本号',
  category_id bigint       NULL COMMENT '栏目ID',
  title       varchar(200) NULL COMMENT '标题',
  summary     varchar(500) NULL COMMENT '摘要',
  content     longtext     NULL COMMENT '正文',
  cover       varchar(255) NULL COMMENT '封面',
  author      varchar(64)  NULL COMMENT '作者',
  is_top      char(1)      NULL COMMENT '置顶(0否1是)',
  status      char(1)      NULL COMMENT '状态(0发布1草稿2下线)',
  sort        int          NULL COMMENT '排序',
  attachment  varchar(255) NULL COMMENT '附件',
  keywords    varchar(255) NULL COMMENT '关键词',
  description varchar(500) NULL COMMENT 'SEO描述',
  publish_time datetime    NULL COMMENT '发布时间',
  update_by   varchar(64)  NULL COMMENT '更新人',
  update_time datetime     NULL COMMENT '更新时间',
  PRIMARY KEY (history_id),
  KEY idx_article_version (article_id, version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文章历史版本（保存前写当前版，最多保留 20 版）';

-- 完成提示
SELECT COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_article' AND COLUMN_NAME = 'version';
