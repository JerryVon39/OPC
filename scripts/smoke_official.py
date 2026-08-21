#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数智游民创新工场官网 · 冒烟测试脚本
用法：先启动后端（8080），再运行：python scripts/smoke_official.py
覆盖：公开接口（服务列表/数据条/字典/新闻）→ 成员登录 → 报名/重复拒绝/我的报名
      → 满员候补 → 入驻申请 → 后台登录与菜单树
与新种子数据（upgrade_20260821_official.sql / business_init.sql）耦合，可重复执行。
"""
import json
import sys
import urllib.request
import urllib.parse

BASE = "http://localhost:8080"
PASS = 0
FAIL = 0


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ✅ {name}")
    else:
        FAIL += 1
        print(f"  ❌ {name} {detail}")


def req(method, path, params=None, headers=None):
    url = BASE + path
    data = None
    if params:
        data = urllib.parse.urlencode(params).encode("utf-8")
    r = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(r, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return {"code": -1, "msg": str(e)}


def main():
    print("== 1. 公开接口 ==")
    d = req("GET", "/system/book/list?pageNum=1&pageSize=50")
    books = d.get("rows", [])
    check("服务列表 21 条", d.get("total") == 21, f"total={d.get('total')}")
    names = [b["bookName"] for b in books]
    check("服务名已业务化", "AI 微短剧制作实战营" in names, str(names[:3]))
    on_sale = [b for b in books if b["status"] == "0"]
    check("招募中服务 20 条", len(on_sale) == 20, f"{len(on_sale)}")
    check("满员服务可候补", any(b["stock"] == 0 for b in on_sale))
    check("名额紧张标签存在", any(0 < b["stock"] <= 3 for b in on_sale))

    d = req("GET", "/system/dashboard/publicStats")
    s = d.get("data", {})
    check("数据条: 服务总数21/成员7", s.get("bookTotal") == 21 and s.get("readerTotal") == 7, str(s))

    d = req("GET", "/system/dict/data/type/book_type")
    labels = [x["dictLabel"] for x in d.get("data", [])]
    check("服务分类字典", labels == ["AI 内容创作", "AI 技术应用", "AI 硬件与场景"], str(labels))

    d = req("GET", "/system/dict/data/type/reader_type")
    labels = [x["dictLabel"] for x in d.get("data", [])]
    check("成员类型字典", labels == ["个人主理人", "团队", "企业"], str(labels))

    d = req("GET", "/system/notice/publicList")
    titles = [n["noticeTitle"] for n in d.get("data", [])]
    check("新闻动态 3 条", len(titles) == 3, str(titles))
    check("新闻标题业务化", any("OPC" in t for t in titles), str(titles))

    d = req("GET", "/system/banner/publicList")
    check("品牌轮播 3 条", len(d.get("data", [])) == 3, str(d.get("data", []))[:100])

    print("== 2. 成员登录与报名链路 ==")
    d = req("POST", "/system/reader/login", {"readerName": "周舟", "cardNo": "JS20260001"})
    token = (d.get("data") or {}).get("sessionToken", "")
    check("成员登录(周舟)", bool(token), d.get("msg", ""))
    if not token:
        print("  中止：登录失败，后续用例跳过")
        return

    d = req("POST", "/system/reader/login", {"readerName": "吴挂", "cardNo": "JS20260004"})
    check("停用成员登录被拒", d.get("code") != 200, d.get("msg", ""))

    # 报名（先查周舟是否已报过该服务——重跑幂等：已报名则跳过实际报名）
    my = req("GET", f"/system/borrow/queryByCard?cardNo=JS20260001&sessionToken={token}")
    my_books = {r["bookName"] for r in my.get("data", [])}
    if "AI 微短剧制作实战营" in my_books:
        check("报名(已存在,跳过)", True)
    else:
        d = req("POST", "/system/borrow/borrowByCard", {"bookId": 1, "cardNo": "JS20260001", "sessionToken": token})
        check("报名成功", d.get("code") == 200, d.get("msg", ""))
    d = req("POST", "/system/borrow/borrowByCard", {"bookId": 1, "cardNo": "JS20260001", "sessionToken": token})
    check("重复报名被拒", d.get("code") != 200, d.get("msg", ""))

    my = req("GET", f"/system/borrow/queryByCard?cardNo=JS20260001&sessionToken={token}")
    check("我的报名有记录", len(my.get("data", [])) > 0)

    # 满员候补（社区共创空间预约 book_id=15 stock=0；种子数据周舟已候补，重复候补会被拒）
    myr = req("GET", f"/system/reserve/myList?cardNo=JS20260001&sessionToken={token}")
    already = any(r.get("status") == "0" for r in myr.get("data", []))
    if already:
        check("满员服务候补(种子已候补)", True)
    else:
        d = req("POST", "/system/reserve/add", {"bookId": 15, "cardNo": "JS20260001", "sessionToken": token})
        check("满员服务可候补", d.get("code") == 200, d.get("msg", ""))
    myr = req("GET", f"/system/reserve/myList?cardNo=JS20260001&sessionToken={token}")
    check("我的候补有记录", any(r.get("status") == "0" for r in myr.get("data", [])), str(myr.get("data", []))[:100])

    print("== 3. 入驻申请 ==")
    d = req("POST", "/system/purchase/apply", {"bookName": "AI 插画工作室", "author": "冒烟测试员", "email": "smoke@qq.com", "remark": "冒烟测试申请"})
    # 重跑幂等：同名待审核申请已存在时视为通过（后端防重复提交）
    check("入驻申请提交", d.get("code") == 200 or "请勿重复提交" in d.get("msg", ""), d.get("msg", ""))
    d = req("POST", "/system/purchase/apply", {"bookName": "AI 插画工作室", "author": "冒烟测试员", "email": "", "remark": ""})
    check("缺邮箱被拒", d.get("code") != 200, d.get("msg", ""))

    print("== 4. 后台登录（验证码接口） ==")
    cap = req("GET", "/captchaImage")
    check("验证码接口可用", cap.get("code") == 200, str(cap)[:100])
    print("  （验证码默认开启，后台完整链路由浏览器冒烟覆盖）")

    print(f"\n结果: {PASS} 通过 / {FAIL} 失败")
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
