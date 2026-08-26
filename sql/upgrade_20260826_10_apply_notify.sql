-- ============================================
-- 65：入驻申请通知运营者——配置键种入（幂等）
-- 键：opc.apply.notify.email（留空=不通知）
-- 后台「系统参数」可改值；新申请落库时向该邮箱发提醒
-- ============================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark)
SELECT '入驻申请通知邮箱', 'opc.apply.notify.email', '', 'Y',
       '65：新入驻申请落库时邮件通知运营者（留空=不通知；逗号分隔支持多邮箱）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'opc.apply.notify.email');
