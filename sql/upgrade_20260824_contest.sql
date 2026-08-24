-- ============================================
-- 创客大赛报名服务条目：contest.html 固定引用 book_id=23
-- 全新部署必须存在该服务，否则「报名参赛」报"无此服务"
-- 幂等：book_id=23 已存在则跳过
-- ============================================
INSERT INTO `book` (book_id, book_name, author, book_type, publisher, price, publish_date, stock, status, isbn, intro, create_by, create_time, update_by, update_time)
SELECT 23, '创客大赛报名', '数智游民创新工场', '2', '清远星谷科技园', 0.00, '2026-08-01', 100, '0', '9780026082402',
  '首届人工智能 OPC 创客短视频创作大赛报名通道：报名即参赛，社区设立创客基金，联动头部 AI 企业对接全国稳定内容订单。',
  'admin', NOW(), '', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM book WHERE book_id = 23);