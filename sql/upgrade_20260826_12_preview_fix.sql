-- ============================================
-- 修复：文章编辑页「前台预览」空白（用户反馈 2026-08-26）
-- 根因：sys_config.site.front.url 残留 dev 期值 http://localhost:8081（无服务端口），
--       预览 iframe 指向该地址加载失败。置空 = 与后台同源（nginx 同域部署默认）。
-- 仅当值恰为 dev 残留时清理，其他自定义值（如独立前台域名）不受影响。
-- ============================================
UPDATE sys_config SET config_value = ''
WHERE config_key = 'site.front.url' AND config_value IN ('http://localhost:8081', 'http://localhost:8080');
