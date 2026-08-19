import urllib.request, urllib.parse, json, subprocess

BASE = 'http://localhost:8080'
import os
MYSQL = r'C:\Users\1\tools\mysql-8.4.9-winx64\bin\mysql.exe'
DB_PASS = os.environ.get('DB_PASS', 'password')

def req(path, method='GET', data=None, headers=None):
    if isinstance(data, str):
        data = data.encode()
    r = urllib.request.Request(BASE + path, method=method, data=data, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'ignore')

def sql(cmd):
    r = subprocess.run([MYSQL, '-uroot', '-p' + DB_PASS, '-N', '--default-character-set=utf8mb4', '-e', 'USE ry-vue; ' + cmd], capture_output=True, encoding='utf-8', errors='replace')
    return (r.stdout or '').strip()

# 预清理：删除上次测试残留的订单并还原库存（保证脚本可重复执行）
sql("DELETE FROM shop_order WHERE order_no LIKE 'WSW178%';")
sql("UPDATE book SET stock=40 WHERE book_name='小王子';")
sql("UPDATE book SET stock=35 WHERE book_name='老人与海';")
sql("UPDATE borrow_record SET due_date='2026-09-12' WHERE reader_id=1 AND book_id=1 AND status='0';")
sql("UPDATE borrow_record SET renew_count=0;")
sql("DELETE FROM borrow_record WHERE borrow_id >= 20;")
sql("UPDATE book SET stock=1 WHERE book_id=15;")

results = []
def tc(no, name, cond, detail=''):
    results.append((no, name, 'PASS' if cond else 'FAIL', detail))
    print('  [%s] %s: %s %s' % (no, name, 'PASS' if cond else 'FAIL', detail))

def login(username, password='admin123'):
    s, d = req('/login', 'POST', json.dumps({'username': username, 'password': password}).encode(), {'Content-Type': 'application/json'})
    return d

SESSION = {}  # cardNo -> 前台登录 sessionToken（登录后提取，业务接口必须携带）

def reader_login(name, card):
    s, d = req('/system/reader/login', 'POST', urllib.parse.urlencode({'readerName': name, 'cardNo': card}).encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
    if d.get('code') == 200:
        SESSION[card] = (d.get('data') or {}).get('sessionToken')
    return d

def auth_params(card):
    return urllib.parse.urlencode({'cardNo': card, 'sessionToken': SESSION.get(card, '')})

# ============ 1. 登录与权限 ============
print('=== 1. 登录与权限 ===')
tc('1.1', 'admin登录', login('admin').get('code') == 200)
for u in ['librarian', 'cashier', 'viewer']:
    tc('1.2', u + '登录', login(u).get('code') == 200)
d = login('viewer')
s, d2 = req('/system/order/list?pageNum=1&pageSize=5', headers={'Authorization': 'Bearer ' + d['token']})
tc('1.3', 'viewer无订单权限', '没有权限' in str(d2.get('msg')))
for name, card, expect in [('学生测试','JS20260001',True), ('教师测试','JS20260002',True), ('普通测试','JS20260003',True), ('Jerry','DK',True), ('挂失测试','JS20260004',False)]:
    d = reader_login(name, card)
    tc('1.4', name + '前台登录', d.get('code') == 200 if expect else d.get('code') != 200, d.get('msg'))
d = reader_login('证号', 'JS20260001')
tc('1.5', '部分姓名登录被拒', d.get('code') != 200)

# ============ 2. 图书 ============
print('=== 2. 图书 ===')
s, d = req('/system/book/list?pageNum=1&pageSize=50')
rows = d.get('rows') or []
tc('2.1', '匿名图书列表(21本全量)', len(rows) == 21)
tc('2.2', '下架书过滤(前台只20本)', len([b for b in rows if b['status'] == '0']) == 20)
tc('2.3', '书名搜索', len((req('/system/book/list?keyword=' + urllib.parse.quote('三体'))[1].get('rows') or [])) > 0)
tc('2.4', '作者搜索', len((req('/system/book/list?keyword=' + urllib.parse.quote('刘慈欣'))[1].get('rows') or [])) > 0)
tc('2.5', 'ISBN搜索', len((req('/system/book/list?keyword=9787020002207')[1].get('rows') or [])) > 0)
s, d = req('/system/book/related?bookId=1&bookType=1')
tc('2.6', '同类推荐', len(d.get('data') or []) > 0)
s, d = req('/system/dict/data/type/book_type')
tc('2.7', '分类字典(3类)', len(d.get('data') or []) == 3)
s, d = req('/system/config/configKey/book.stock.warn')
tc('2.8', '库存预警参数', d.get('msg') == '3')

# ============ 3. 读者 ============
print('=== 3. 读者 ===')
s, d = req('/system/reader/register', 'POST', urllib.parse.urlencode({'readerName': '整体测试', 'phone': '13800008888', 'readerType': '1', 'remark': ''}).encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
rid = (d.get('data') or {}).get('readerId')
new_card = (d.get('data') or {}).get('cardNo')
tc('3.1', '前台登记(证号后端生成)', d.get('code') == 200 and str(new_card).startswith('JS'), str(new_card))
d = reader_login('整体测试', new_card)
tc('3.2', '新登记可登录', d.get('code') == 200)
s, d = req('/system/reader/updateMyInfo', 'POST', (auth_params(new_card) + '&readerName=' + urllib.parse.quote('整体测试') + '&phone=13800009999').encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
tc('3.3', '修改手机号', d.get('code') == 200)
s, d = req('/system/reader/updateMyInfo', 'POST', (auth_params(new_card) + '&readerName=' + urllib.parse.quote('错名') + '&phone=13800000000').encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
tc('3.4', '错误姓名改手机被拒', d.get('code') != 200)
# 清理测试读者
_, da = req('/login', 'POST', json.dumps({'username': 'admin', 'password': 'admin123'}).encode(), {'Content-Type': 'application/json'})
auth = {'Authorization': 'Bearer ' + da['token']}
s, d = req('/system/reader/' + str(rid), 'DELETE', headers=auth)
tc('3.5', '删除无关联读者', d.get('code') == 200)

# ============ 4. 借阅 ============
print('=== 4. 借阅 ===')
# 4.1 欠费冻结（普通测试欠费1.20）
d = reader_login('普通测试', 'JS20260003')
s, d = req('/system/borrow/borrowByCard', 'POST', auth_params('JS20260003') + '&bookId=10', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('4.1', '欠费冻结借书被拒', '罚款' in str(d.get('msg')), d.get('msg'))
# 4.2 正常借书（学生借西游记）
s, d = req('/system/borrow/borrowByCard', 'POST', auth_params('JS20260001') + '&bookId=10', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('4.2', '正常借书', d.get('code') == 200, d.get('msg'))
bid = sql("SELECT MAX(borrow_id) FROM borrow_record WHERE reader_id=1 AND book_id=10;")
# 4.3 重复借阅被拒
s, d = req('/system/borrow/borrowByCard', 'POST', auth_params('JS20260001') + '&bookId=10', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('4.3', '重复借阅被拒', '未归还' in str(d.get('msg')), d.get('msg'))
# 4.4 我的借阅
s, d = req('/system/borrow/queryByCard?' + auth_params('JS20260001'))
tc('4.4', '我的借阅列表', d.get('code') == 200 and len(d.get('data') or []) >= 3)
# 4.5 教师借期60天（教师借三体？学生借了……借围城 book7）
s, d = req('/system/borrow/borrowByCard', 'POST', auth_params('JS20260002') + '&bookId=7', {'Content-Type': 'application/x-www-form-urlencoded'})
due_t = sql("SELECT due_date FROM borrow_record WHERE reader_id=2 ORDER BY borrow_id DESC LIMIT 1;")
from datetime import date, timedelta
borrow_d = sql("SELECT borrow_date FROM borrow_record WHERE reader_id=2 ORDER BY borrow_id DESC LIMIT 1;")
expect_due = (date(int(borrow_d[:4]), int(borrow_d[5:7]), int(borrow_d[8:10])) + timedelta(days=60)).isoformat()
tc('4.5', '教师借期60天', due_t == expect_due, due_t + '(期望' + expect_due + ')')
# 4.6 还书 + 罚款 + 预约联动（学生还西游记，检查白夜行预约？西游记无预约——还书正常即可）
s, d = req('/system/borrow/return/' + bid, 'PUT', headers=auth)
tc('4.6', '还书正常', d.get('code') == 200, d.get('msg'))
# 清理教师借的围城
bid_t = sql("SELECT MAX(borrow_id) FROM borrow_record WHERE reader_id=2 AND book_id=7;")
s, d = req('/system/borrow/return/' + bid_t, 'PUT', headers=auth)
# 4.7 续借（学生借三体→续借）
s, d = req('/system/borrow/renewByCard', 'POST', auth_params('JS20260001') + '&borrowId=' + sql("SELECT borrow_id FROM borrow_record WHERE reader_id=1 AND book_id=1 AND status='0' LIMIT 1;"), {'Content-Type': 'application/x-www-form-urlencoded'})
tc('4.7', '前台续借', d.get('code') == 200, d.get('msg'))

# ============ 5. 预约 ============
print('=== 5. 预约 ===')
sql("UPDATE book SET stock=0 WHERE book_id=16;")
s, d = req('/system/reserve/add', 'POST', auth_params('JS20260001') + '&bookId=16', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('5.1', '预约成功', d.get('code') == 200, d.get('msg'))
s, d = req('/system/reserve/add', 'POST', auth_params('JS20260001') + '&bookId=16', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('5.2', '重复预约拒绝', '重复预约' in str(d.get('msg')))
s, d = req('/system/reserve/myList?' + auth_params('JS20260001'))
has16 = any(r.get('bookId') == 16 for r in (d.get('data') or []))
tc('5.3', '我的预约含新预约', has16)
rid16 = sql("SELECT reserve_id FROM book_reserve WHERE reader_id=1 AND book_id=16;")
s, d = req('/system/reserve/cancel', 'POST', auth_params('JS20260001') + '&reserveId=' + rid16, {'Content-Type': 'application/x-www-form-urlencoded'})
tc('5.4', '取消预约', sql("SELECT status FROM book_reserve WHERE reserve_id=" + rid16 + ";") == '3')
sql("UPDATE book SET stock=18 WHERE book_id=16;")
sql("DELETE FROM book_reserve WHERE book_id=16;")

# ============ 6. 订单 ============
print('=== 6. 订单 ===')
s, d = req('/system/order/create', 'POST', auth_params('JS20260001') + '&bookId=13&quantity=2', {'Content-Type': 'application/x-www-form-urlencoded'})
tc('6.1', '下单成功', d.get('code') == 200, d.get('msg'))
oid = sql("SELECT MAX(order_id) FROM shop_order;")
stock_after = sql("SELECT stock FROM book WHERE book_id=13;")
tc('6.2', '下单库存-2', stock_after == '38', 'stock=' + stock_after)
s, d = req('/system/order/queryByCard?' + auth_params('JS20260001'))
tc('6.3', '我的订单', d.get('code') == 200)
s, d = req('/system/order', 'PUT', json.dumps({'orderId': int(oid), 'status': '3'}).encode(), {**auth, 'Content-Type': 'application/json'})
tc('6.4', '订单收款(0→3)', d.get('code') == 200, d.get('msg'))
s, d = req('/system/order', 'PUT', json.dumps({'orderId': int(oid), 'status': '2'}).encode(), {**auth, 'Content-Type': 'application/json'})
tc('6.5', '已收款不可取消', '不允许' in str(d.get('msg')), d.get('msg'))
s, d = req('/system/order', 'PUT', json.dumps({'orderId': int(oid), 'status': '1'}).encode(), {**auth, 'Content-Type': 'application/json'})
tc('6.6', '收款后完成(3→1)', d.get('code') == 200, d.get('msg'))
# 取消回滚测试
s, d = req('/system/order/create', 'POST', auth_params('JS20260001') + '&bookId=14&quantity=1', {'Content-Type': 'application/x-www-form-urlencoded'})
oid2 = sql("SELECT MAX(order_id) FROM shop_order;")
stock_mid = sql("SELECT stock FROM book WHERE book_id=14;")
s, d = req('/system/order/cancelByCard', 'POST', auth_params('JS20260001') + '&orderId=' + oid2, {'Content-Type': 'application/x-www-form-urlencoded'})
stock_back = sql("SELECT stock FROM book WHERE book_id=14;")
tc('6.7', '取消订单回滚库存', stock_back == str(int(stock_mid) + 1), stock_mid + '->' + stock_back)

# ============ 7. 后台 ============
print('=== 7. 后台 ===')
s, d = req('/system/dashboard/stats', headers=auth)
st = d.get('data') or {}
tc('7.1', '看板统计(10项)', len(st) >= 10 and st.get('bookTotal') == 21)
s, d = req('/system/borrow/stats')
tc('7.2', '借阅统计', len((d.get('data') or {}).get('topBooks') or []) > 0)
s, d = req('/system/borrow/list?pageNum=1&pageSize=5', headers=auth)
tc('7.3', '借阅列表', d.get('code') == 200)
s, d = req('/system/order/list?pageNum=1&pageSize=5', headers=auth)
tc('7.4', '订单列表', d.get('code') == 200)
try:
    body = urllib.request.urlopen(urllib.request.Request(BASE + '/system/borrow/export', method='POST', headers=auth), timeout=20).read()
    tc('7.5', '借阅导出xlsx', body[:2] == b'PK')
except Exception as e:
    tc('7.5', '借阅导出xlsx', False, str(e))
s, d = req('/system/notice/publicList')
tc('7.6', '公告列表', len(d.get('data') or []) > 0)
s, d = req('/system/reader/reissue/4', 'PUT', headers=auth)
tc('7.7', '后台补办', d.get('data') is not None, str(d.get('data')))
sql("UPDATE reader SET status='1' WHERE reader_id=4;")

# ============ 8. 数据一致性 ============
print('=== 8. 数据一致性 ===')
# 删除有未还借阅的图书被拒
s, d = req('/system/book/1', 'DELETE', headers=auth)
tc('8.1', '删除有未还借阅的书被拒', '未归还' in str(d.get('msg')), d.get('msg'))
# 删除有未还借阅的读者被拒
s, d = req('/system/reader/1', 'DELETE', headers=auth)
tc('8.2', '删除有未还借阅的读者被拒', '未归还' in str(d.get('msg')), d.get('msg'))

# ============ 9. 新功能回归（搜索联想/排序/荐购/上下架） ============
print('=== 9. 新功能回归 ===')
# 9.1 搜索联想：输入"三"联想出《三体》
s, d = req('/system/book/suggest?keyword=' + urllib.parse.quote('三'))
names = [b.get('bookName') for b in (d.get('data') or [])]
tc('9.1', '搜索联想返回三体', '三体' in names, str(names[:3]))
# 9.2 借阅最多排序：borrowCount 降序
s, d = req('/system/book/list?pageNum=1&pageSize=10&orderByColumn=borrowCount&isAsc=desc')
rows = d.get('rows') or []
cnts = [r.get('borrowCount') or 0 for r in rows]
tc('9.2', '借阅最多降序', d.get('code') == 200 and cnts == sorted(cnts, reverse=True), 'counts=' + str(cnts[:5]))
# 9.3 最新出版排序：publishDate 降序
s, d = req('/system/book/list?pageNum=1&pageSize=10&orderByColumn=publishDate&isAsc=desc')
rows = d.get('rows') or []
dates = [str(r.get('publishDate') or '') for r in rows]
tc('9.3', '最新出版降序', dates == sorted(dates, reverse=True), 'dates=' + str(dates[:3]))
# 9.4 排序字段白名单：非法字段被拒
s, d = req('/system/book/list?pageNum=1&pageSize=5&orderByColumn=evil%20union')
tc('9.4', '非法排序字段被拒', '非法的排序字段' in str(d), str(d)[:80])
# 9.4a isAsc 白名单：经 isAsc 旁路排序任意列被拒
s, d = req('/system/book/list?pageNum=1&pageSize=5&orderByColumn=bookId&isAsc=desc%2C%20intro')
tc('9.4a', '非法排序方向被拒', '非法的排序方向' in str(d), str(d)[:80])
# 9.5 匿名提交荐购成功
import time as _t
_pname = '回归测试荐购书' + str(int(_t.time()))
s, d = req('/system/purchase/apply', 'POST', urllib.parse.urlencode({'bookName': _pname, 'author': '回归', 'remark': 'auto'}).encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
tc('9.5', '匿名提交荐购', d.get('code') == 200, d.get('msg'))
# 9.6 同书名待处理去重
s, d = req('/system/purchase/apply', 'POST', urllib.parse.urlencode({'bookName': _pname}).encode(), {'Content-Type': 'application/x-www-form-urlencoded'})
tc('9.6', '荐购去重拒绝', '重复提交' in str(d.get('msg')), d.get('msg'))
# 9.7 后台荐购列表 + 处理
s, d = req('/system/purchase/list?pageNum=1&pageSize=10', headers=auth)
row = next((r for r in (d.get('rows') or []) if r.get('bookName') == _pname), None)
tc('9.7', '后台荐购列表可见', row is not None, str(row))
if row:
    s, d = req('/system/purchase', 'PUT', json.dumps({'reqId': row['reqId'], 'bookName': row['bookName'], 'author': row.get('author'), 'status': '1'}).encode(), dict(auth, **{'Content-Type': 'application/json'}))
    s, d2 = req('/system/purchase/list?pageNum=1&pageSize=10', headers=auth)
    row2 = next((r for r in (d2.get('rows') or []) if r.get('bookName') == _pname), None)
    tc('9.8', '后台处理荐购(已处理)', row2 is not None and row2.get('status') == '1', str(row2))
# 9.9 有预约中的书禁止下架（白夜行 book 15）
s, d = req('/system/book/changeStatus?bookId=15&status=1', 'PUT', headers=auth)
tc('9.9', '预约中的书禁止下架', '预约' in str(d.get('msg')), d.get('msg'))
# 9.10 正常书上下架（明朝那些事儿 book 3：下架→上架→还原下架）
s, d = req('/system/book/changeStatus?bookId=3&status=0', 'PUT', headers=auth)
s2, d2 = req('/system/book/changeStatus?bookId=3&status=1', 'PUT', headers=auth)
tc('9.10', '开关上架/下架正常', d.get('code') == 200 and d2.get('code') == 200, d.get('msg'))
# 9.11 清理荐购测试数据
sql("DELETE FROM book_purchase_req WHERE book_name='" + _pname + "';")

# ============ 10. 批量导入 + 库存预警 ============
print('=== 10. 批量导入 + 库存预警 ===')
try:
    from openpyxl import Workbook
    def make_xlsx(headers, rows, path):
        wb = Workbook()
        ws = wb.active
        ws.append(headers)           # 第 1 行列名（ExcelUtil 按列顺序解析，titleNum=0 不跳行）
        for r in rows:
            ws.append(r)
        wb.save(path)

    def upload_xlsx(path, url, tok):
        boundary = '----WSWBoundary' + 'x' * 12
        with open(path, 'rb') as f:
            content = f.read()
        body = ('--' + boundary + '\r\n').encode() + \
            'Content-Disposition: form-data; name="file"; filename="import.xlsx"\r\n'.encode() + \
            b'Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n' + \
            content + b'\r\n' + ('--' + boundary + '--\r\n').encode()
        r = urllib.request.Request(BASE + url, data=body, method='POST',
                                   headers={'Content-Type': 'multipart/form-data; boundary=' + boundary, 'Authorization': 'Bearer ' + tok})
        with urllib.request.urlopen(r, timeout=30) as x:
            return json.loads(x.read())

    # 10.1 图书导入：1 新书 + 1 重名跳过 + 1 空名跳过
    make_xlsx(['图书名称', '作者', '图书类型(字典:book_type)', '出版社', '价格(元)', '出版日期', '库存数量', '状态(0在架 1下架)'],
              [['导入测试书A', '测试作者', '1', '测试社', 25.5, '2026-08-18', 5, '0'],
               ['三体', '刘慈欣', '1', '重庆出版社', 88, '2008-01-01', 9, '0'],
               [None, '无名', '1', '测试社', 10, '2026-08-18', 1, '0']], '.import_book.xlsx')
    d = upload_xlsx('.import_book.xlsx', '/system/book/importData', auth['Authorization'].split(' ')[1])
    data = d.get('data') or {}
    tc('10.1', '图书导入成功1跳过2', data.get('success') == 1 and data.get('fail') == 2, str(data.get('errors'))[:100])
    sql("DELETE FROM book WHERE book_name='导入测试书A';")

    # 10.2 读者导入：1 新读者（证号自动生成）+ 1 手机号错
    make_xlsx(['读者姓名', '手机号码', '借书证号', '读者类型', '性别(0男 1女 2未知)', '出生日期', '状态(0正常 1停用)'],
              [['导入测试读者', '13900001111', '', '1', '0', '2000-01-01', '0'],
               ['坏手机号', 'abc', '', '1', '0', '2000-01-01', '0']], '.import_reader.xlsx')
    d = upload_xlsx('.import_reader.xlsx', '/system/reader/importData', auth['Authorization'].split(' ')[1])
    data = d.get('data') or {}
    card = sql("SELECT card_no FROM reader WHERE reader_name='导入测试读者' LIMIT 1;")
    tc('10.2', '读者导入成功1失败1且证号生成', data.get('success') == 1 and data.get('fail') == 1 and card.startswith('JS'), str(data.get('errors'))[:100])
    sql("DELETE FROM reader WHERE reader_name='导入测试读者';")

    # 10.3 库存预警：临时置库存 1 → 出现在看板 lowStockBooks → 还原
    sql("UPDATE book SET stock=1 WHERE book_name='万历十五年';")
    s, d = req('/system/dashboard/stats', headers=auth)
    low = (d.get('data') or {}).get('lowStockBooks') or []
    hit = any(b.get('bookName') == '万历十五年' for b in low)
    sql("UPDATE book SET stock=20 WHERE book_name='万历十五年';")
    tc('10.3', '库存预警列表含低库存书', isinstance(low, list) and hit, str(low)[:120])
    import os
    for f in ['.import_book.xlsx', '.import_reader.xlsx']:
        if os.path.exists(f):
            os.remove(f)
except Exception as e:
    tc('10.1', '导入/预警章节执行', False, str(e))

# ============ 汇总 ============
total = len(results)
passed = sum(1 for r in results if r[2] == 'PASS')
print()
print('=== 整体测试汇总: %d/%d 通过 ===' % (passed, total))
for r in results:
    if r[2] == 'FAIL':
        print('  FAIL:', r[0], r[1], r[3])
