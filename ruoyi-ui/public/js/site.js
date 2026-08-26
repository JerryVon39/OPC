/* ============================================================
 * 数智游民创新工场 · 前台公共脚本（单一来源）
 * 认证（登录/两步注册/找回密码/登出/会话）、通用工具、登录态渲染。
 * 所有页面通过 <script src="js/site.js"></script> 引入，
 * 页面特有逻辑通过钩子接入：
 *   window.AUTH_AFTER_LOGIN  = () => {...}  登录成功后调用（刷新本页数据）
 *   window.AUTH_AFTER_LOGOUT = () => {...}  登出后调用
 * 修改本文件即全站生效，禁止在页面内再复制这些函数。
 * ============================================================ */

// ===== 认证相关接口 =====
const REG_API = '/prod-api/system/reader/register';
const LOGIN_API = '/prod-api/system/reader/login';
const VERIFY_EMAIL_API = '/prod-api/system/reader/verify-email';
const RESEND_CODE_API = '/prod-api/system/reader/resend-code';
const LOGOUT_API = '/prod-api/system/reader/logout';
const FORGOT_API = '/prod-api/system/reader/forgot-password';
const RESET_PWD_API = '/prod-api/system/reader/reset-password';
const MYINFO_API = '/prod-api/system/reader/updateMyInfo';

// ===== 后台预览模式（页面搭建/区块管理内嵌 iframe 使用）=====
// ?preview=1：模块/区块容器加虚线框+角标（显示 section_key / block_key）
// ?highlight=key：高亮该模块并滚动定位；后台也可 postMessage 发送滚动定位
window.IS_PREVIEW = new URLSearchParams(location.search).get('preview') === '1';
window.PREVIEW_HIGHLIGHT = new URLSearchParams(location.search).get('highlight') || '';

// ===== 登录态（localStorage 持久化）=====
let currentUser = null;
try {
  const saved = localStorage.getItem('shopUser');
  if (saved) currentUser = JSON.parse(saved);
} catch (e) {}

const sessionToken = () => currentUser && currentUser.sessionToken ? currentUser.sessionToken : '';

// ===== 通用工具 =====
function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
// 链接协议白名单（对齐 home.html goBannerLink 思路）：放行 http(s)/绝对与相对路径/锚点/mailto，
// 其余一律落 '#' —— esc() 只转义 HTML 字符，拦不住 javascript: 直通（H3 修复）。
// 注意：相对路径（join.html、home.html#contact 等种子数据常用）无协议头，按"不含协议头即安全"放行
function safeLink(link) {
  const s = String(link == null ? '' : link).trim();
  if (!s) return '#';
  if (/^(https?:\/\/|\/|#|mailto:)/i.test(s)) return s;
  if (!/^[a-z][a-z0-9+.-]*:/i.test(s)) return s;
  return '#'; // javascript:/data:/vbscript: 等危险协议一律拦截
}
// 正整数列数：非法值一律回退 3 —— 防 style 属性注入（H3 修复）
function safeCols(v) {
  return /^[1-9]\d*$/.test(String(v == null ? '' : v)) ? v : 3;
}
// canonical 占位域名修正（H7 修复）：9 个静态页 <link rel="canonical"> 原指保留域
// opc.example.com，部署到正式域名后收录/权重会被引走；此处按当前站点地址改写
(function () {
  var link = document.querySelector('link[rel="canonical"]');
  if (!link || !/opc\.example\.com/i.test(link.href)) return;
  link.href = location.origin + location.pathname;
})();
// 导航防闪烁 + 高亮保持（站点设置排序即时生效）：
// ① 缓存新鲜（≤60s）→ 同步渲染并重跑 initNav 高亮（脚本在 body 末尾执行、早于首绘，切页零闪烁）
// ② 缓存缺失/过期 → 先隐藏静态导航（nav-pending，visibility:hidden 不占位抖动），
//    配置到达后再显示——绝不闪一下"旧顺序"；请求失败 3.5s 后兜底恢复静态导航
(function () {
  const box = document.getElementById('navAnchors');
  if (!box) return;
  const render = (items) => {
    const html = items.map(n => '<a href="' + esc(safeLink(n.link)) + '">' + esc(n.name || '') + '</a>').join('');
    if (html) box.innerHTML = html;
    if (typeof initNav === 'function') initNav(); // 渲染后重跑 URL 高亮（替换 innerHTML 会丢 active 类）
  };
  try {
    const raw = localStorage.getItem('opc_site_nav');
    if (raw) {
      let items = null, t = 0;
      try {
        const c = JSON.parse(raw);
        if (Array.isArray(c)) { items = c; }               // 旧格式（无时间戳）：视为过期
        else if (c && Array.isArray(c.items)) { items = c.items; t = c.t || 0; }
      } catch (e) {}
      if (Array.isArray(items) && items.length && Date.now() - t <= 60000) {
        render(items);
        return;
      }
    }
  } catch (e) { /* 隐私模式等：走隐藏-兜底路径 */ }
  box.classList.add('nav-pending');
  // 兜底（请求失败/超时）：优先渲染任意缓存（即使过期——上次配置也好过静态初版顺序），
  // 无缓存才恢复静态导航——任何情况下都不闪"静态初版"
  setTimeout(function () {
    let items = null;
    try {
      const raw = localStorage.getItem('opc_site_nav');
      if (raw) { const c = JSON.parse(raw); items = Array.isArray(c) ? c : (c && Array.isArray(c.items) ? c.items : null); }
    } catch (e) {}
    if (Array.isArray(items) && items.length) {
      const html = items.map(n => '<a href="' + esc(safeLink(n.link)) + '">' + esc(n.name || '') + '</a>').join('');
      if (html) box.innerHTML = html;
      if (typeof initNav === 'function') initNav();
    }
    box.classList.remove('nav-pending');
  }, 3500);
})();
function closeModal(id) { const el = document.getElementById(id); if (el) el.style.display = 'none'; }
function toast(text) {
  const t = document.getElementById('toast');
  if (!t) return;
  let icon = 'ℹ️';
  if (/失败|错误|已取消|不可|无法|请先/.test(text)) icon = '❌';
  else if (/成功|欢迎|已退出|已保存/.test(text)) icon = '✅';
  t.textContent = icon + ' ' + text;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2500);
}

// ===== 统一请求包装：自动附带 X-Session-Token 头（凭证不落 URL/日志）=====
const apiFetch = (url, options) => {
  options = options || {};
  const headers = new Headers(options.headers || {});
  const tok = sessionToken();
  if (tok) headers.set('X-Session-Token', tok);
  options.headers = headers;
  return fetch(url, options);
};

// ===== 登录态渲染（登录/退出入口 + 个人主页链接）=====
function renderLoginState() {
  const area = document.getElementById('loginArea');
  if (!area) return;
  area.innerHTML = currentUser
    ? '<span style="cursor:pointer" onclick="logout()">你好，' + esc(currentUser.readerName) + '（退出）</span> <a href="profile.html" class="nav-profile-link"><span class="pf-icon">👤</span>个人主页</a>'
    : '<span style="cursor:pointer" onclick="openLogin()">👤 登录</span>';
  const regEntry = document.getElementById('registerEntry');
  if (regEntry) regEntry.style.display = currentUser ? 'none' : '';
}

// ===== 登录（成员编号/手机号/邮箱 + 密码）=====
function openLogin() { document.getElementById('loginModal').style.display = 'flex'; }
/** 密码强度：≥10 位且含大小写字母/数字/符号至少 3 类（与后端 PasswordStrength.check 一致） */
function passwordStrengthOk(pwd) {
  if (!pwd || pwd.length < 10) return false;
  let cls = 0;
  if (/[a-z]/.test(pwd)) cls++;
  if (/[A-Z]/.test(pwd)) cls++;
  if (/[0-9]/.test(pwd)) cls++;
  if (/[^a-zA-Z0-9]/.test(pwd)) cls++;
  return cls >= 3;
}
async function submitLogin() {
  const card = document.getElementById('loginCard').value.trim();
  const pwd = document.getElementById('loginPwd').value;
  const msg = document.getElementById('loginMsg');
  const btn = document.getElementById('loginSubmit');
  if (!card || !pwd) { msg.textContent = '请输入成员编号/手机号/邮箱和密码'; msg.style.color = '#c65d43'; return; }
  if (btn && btn.disabled) return; // 防重复提交
  if (btn) btn.disabled = true;
  try {
    const body = new URLSearchParams({ account: card, password: pwd });
    const res = await apiFetch(LOGIN_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    if (d.code === 200 && d.data) {
      currentUser = d.data;
      localStorage.setItem('shopUser', JSON.stringify(currentUser));
      renderLoginState();
      closeModal('loginModal');
      msg.textContent = '';
      toast('登录成功，欢迎 ' + currentUser.readerName + '！');
      if (currentUser.emailVerified === false) {
        setTimeout(() => toast('提示：您的邮箱尚未验证，建议在个人主页「账号安全」中验证'), 2000);
      }
      // 页面钩子：登录后刷新本页数据（如报名状态）
      if (typeof window.AUTH_AFTER_LOGIN === 'function') { try { window.AUTH_AFTER_LOGIN(); } catch (e) {} }
    } else if (d.code === 601) {
      // 存量成员首次登录：引导找回密码（邮箱验证 → 设置密码）
      closeModal('loginModal');
      toast('该账号需先验证邮箱并设置密码，请按提示操作');
      setTimeout(() => openForgot(), 400);
    } else {
      msg.textContent = d.msg || '登录失败';
      msg.style.color = '#c65d43';
    }
  } catch (e) {
    msg.textContent = '登录失败：' + e.message;
    msg.style.color = '#c65d43';
  } finally {
    if (btn) btn.disabled = false;
  }
}

// ===== 登出 =====
function logout() {
  // 通知后端删除会话（尽力而为，失败不影响本地登出）
  apiFetch(LOGOUT_API, { method: 'POST' }).catch(() => {});
  currentUser = null;
  localStorage.removeItem('shopUser');
  renderLoginState();
  toast('已退出登录');
  if (typeof window.AUTH_AFTER_LOGOUT === 'function') { try { window.AUTH_AFTER_LOGOUT(); } catch (e) {} }
}

// 会话过期统一处理（区别于"暂无记录"：引导重新登录而非误以为数据丢失）
function handleSessionExpired() {
  currentUser = null;
  localStorage.removeItem('shopUser');
  renderLoginState();
  toast('登录已失效，请重新登录');
  setTimeout(() => openLogin(), 400);
  if (typeof window.AUTH_AFTER_LOGOUT === 'function') { try { window.AUTH_AFTER_LOGOUT(); } catch (e) {} }
}

// ===== 两步注册（资料+密码 → 邮箱验证码）=====
let pendingReg = null; // { cardNo, email }
function openRegister() { document.getElementById('regModal').style.display = 'flex'; }
async function submitRegister() {
  const name = document.getElementById('regName').value.trim();
  const phone = document.getElementById('regPhone').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const type = document.getElementById('regType').value;
  const pwd = document.getElementById('regPwd').value;
  const pwd2 = document.getElementById('regPwd2').value;
  const msg = document.getElementById('regMsg');
  if (!name || !type) { msg.textContent = '请填写姓名和成员类型'; msg.style.color = '#c65d43'; return; }
  if (!phone || !/^\d{11}$/.test(phone)) { msg.textContent = '请填写 11 位手机号'; msg.style.color = '#c65d43'; return; }
  if (!email || !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(email)) { msg.textContent = '请填写有效的电子邮箱（用于验证与通知）'; msg.style.color = '#c65d43'; return; }
  if (!pwd || pwd !== pwd2) { msg.textContent = '两次输入的密码不一致'; msg.style.color = '#c65d43'; return; }
  if (!passwordStrengthOk(pwd)) { msg.textContent = '密码长度至少 10 位，且含大小写/数字/符号至少 3 类'; msg.style.color = '#c65d43'; return; }
  try {
    // 成员编号由后端生成（防伪造/占坑）；第一步提交后向邮箱发验证码
    const res = await apiFetch(REG_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ readerName: name, phone: phone, email: email, readerType: type, remark: document.getElementById('regRemark').value.trim(), password: pwd }).toString()
    });
    const d = await res.json();
    if (d.code === 200 && d.data) {
      pendingReg = { cardNo: d.data.cardNo, email: d.data.email };
      document.getElementById('regNewCard').textContent = d.data.cardNo;
      document.getElementById('regStep1').style.display = 'none';
      document.getElementById('regStep2').style.display = '';
      msg.textContent = '验证码已发送至 ' + d.data.email + '（如未收到可点重发）';
      msg.style.color = '#4f8a62';
    } else {
      msg.textContent = d.msg || '注册失败';
      msg.style.color = '#c65d43';
    }
  } catch (e) {
    msg.textContent = '注册失败：' + e.message;
    msg.style.color = '#c65d43';
  }
}
async function verifyRegister() {
  const code = document.getElementById('regCode').value.trim();
  const msg = document.getElementById('regMsg');
  if (!pendingReg || !code) { msg.textContent = '请输入邮箱验证码'; msg.style.color = '#c65d43'; return; }
  try {
    const body = new URLSearchParams({ cardNo: pendingReg.cardNo, email: pendingReg.email, code: code });
    const res = await apiFetch(VERIFY_EMAIL_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    if (d.code === 200) {
      closeModal('regModal');
      pendingReg = null;
      toast('注册成功！请用成员编号 + 密码登录');
      setTimeout(() => openLogin(), 400);
    } else {
      msg.textContent = d.msg || '验证失败';
      msg.style.color = '#c65d43';
    }
  } catch (e) { msg.textContent = '验证失败：' + e.message; msg.style.color = '#c65d43'; }
}
async function resendRegCode() {
  const msg = document.getElementById('regMsg');
  if (!pendingReg) return;
  try {
    const body = new URLSearchParams({ target: pendingReg.email, purpose: 'register' });
    const res = await apiFetch(RESEND_CODE_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    msg.textContent = d.msg || '已重新发送';
    msg.style.color = d.code === 200 ? '#4f8a62' : '#c65d43';
  } catch (e) { msg.textContent = '发送失败：' + e.message; msg.style.color = '#c65d43'; }
}

// ===== 找回密码（证号/手机号/邮箱 → 邮箱验证码 → 新密码）=====
function openForgot() {
  closeModal('loginModal');
  document.getElementById('forgotModal').style.display = 'flex';
}
async function sendForgotCode() {
  const card = document.getElementById('fgCard').value.trim();
  const msg = document.getElementById('forgotMsg');
  if (!card) { msg.textContent = '请输入成员编号/手机号/邮箱'; msg.style.color = '#c65d43'; return; }
  try {
    const body = new URLSearchParams({ account: card });
    const res = await apiFetch(FORGOT_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    msg.textContent = d.msg || '验证码已发送';
    msg.style.color = d.code === 200 ? '#4f8a62' : '#c65d43';
  } catch (e) { msg.textContent = '发送失败：' + e.message; msg.style.color = '#c65d43'; }
}
async function submitReset() {
  const card = document.getElementById('fgCard').value.trim();
  const code = document.getElementById('fgCode').value.trim();
  const pwd = document.getElementById('fgPwd').value;
  const pwd2 = document.getElementById('fgPwd2').value;
  const msg = document.getElementById('forgotMsg');
  if (!card || !code) { msg.textContent = '请输入账号和验证码'; msg.style.color = '#c65d43'; return; }
  if (!pwd || pwd !== pwd2) { msg.textContent = '两次输入的密码不一致'; msg.style.color = '#c65d43'; return; }
  if (!passwordStrengthOk(pwd)) { msg.textContent = '密码长度至少 10 位，且含大小写/数字/符号至少 3 类'; msg.style.color = '#c65d43'; return; }
  try {
    const body = new URLSearchParams({ account: card, code: code, newPassword: pwd });
    const res = await apiFetch(RESET_PWD_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    if (d.code === 200) {
      closeModal('forgotModal');
      toast('密码已重置，请重新登录');
      setTimeout(() => openLogin(), 400);
    } else {
      msg.textContent = d.msg || '重置失败';
      msg.style.color = '#c65d43';
    }
  } catch (e) { msg.textContent = '重置失败：' + e.message; msg.style.color = '#c65d43'; }
}

// ===== 修改手机号（公共；邮箱修改走个人主页「账号安全」验证码流程）=====
async function updateMyInfo() {
  const phone = document.getElementById('myPhone').value.trim();
  const msg = document.getElementById('myInfoMsg');
  if (!currentUser) { msg.textContent = '请先登录'; msg.style.color = '#c65d43'; return; }
  if (!phone || !/^\d{11}$/.test(phone)) { msg.textContent = '请输入 11 位手机号'; msg.style.color = '#c65d43'; return; }
  try {
    const body = new URLSearchParams({ cardNo: currentUser.cardNo, readerName: currentUser.readerName, phone: phone });
    const res = await apiFetch(MYINFO_API, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: body.toString() });
    const d = await res.json();
    if (d.code === 200) {
      currentUser.phone = phone;
      localStorage.setItem('shopUser', JSON.stringify(currentUser));
      msg.textContent = '手机号修改成功';
      msg.style.color = '#4f8a62';
    } else {
      if (d.msg && d.msg.indexOf('登录已失效') >= 0) { handleSessionExpired(); return; }
      msg.textContent = d.msg || '修改失败';
      msg.style.color = '#c65d43';
    }
  } catch (e) { msg.style.color = '#c65d43'; msg.textContent = '修改失败：' + e.message; }
}

// ===== 导航（统一版 navbar 的交互与高亮）=====
// 汉堡菜单展开/收起（移动端导航）
function toggleNavMenu(e) {
  e.stopPropagation();
  const nl = document.getElementById('navAnchors');
  if (nl) nl.classList.toggle('open');
}
// "更多"下拉展开/收起（注册/后台管理入口）
function toggleMoreMenu(e) {
  e.stopPropagation();
  const m = document.getElementById('navMore');
  if (m) m.classList.toggle('open');
}
// 进入后台管理（Vue 后台，未登录自动跳登录页）
function goAdmin() { location.href = '/index'; }

// 按当前 URL 自动高亮导航（各页不再手工标 active）
// 锚点互斥：URL 带锚点时只高亮同锚点链接（如 home.html#contact 联系我们），
// 无锚点链接（如首页 home.html）不再参与匹配——避免「首页」与「联系我们」
// 指向同一文件时同步高亮；无锚点 URL 则按文件名匹配
function initNav() {
  const raw = location.pathname.split('/').pop() || 'home.html';
  const path = raw.split('#')[0];
  const anchor = location.hash; // '' 或 '#contact'
  const links = document.querySelectorAll('.nav-links a');
  links.forEach(a => a.classList.remove('active')); // 先清旧高亮（hashchange 重跑时防止残留）
  links.forEach(a => {
    const href = a.getAttribute('href') || '';
    const file = href.split('#')[0];
    const frag = href.includes('#') ? href.slice(href.indexOf('#')) : '';
    const isHomeAlias = (path === 'index.html' || path === '') && file === 'home.html';
    if (anchor) {
      if (frag === anchor) {
        a.classList.add('active'); // 带锚点 URL：只高亮锚点链接（「联系我们」）
      }
    } else if (!frag && (file === path || isHomeAlias)) {
      a.classList.add('active'); // 无锚点 URL：按文件名匹配（「首页」等）
    }
  });
}
// 同页锚点跳转（#contact）不重载页面：hash 变化时重新高亮
window.addEventListener('hashchange', initNav);

// 长滚动首页滚动联动：滚到「联系我们」区块高亮「联系我们」，离开回到「首页」。
// 仅存在 #contact 区块的页面（首页）生效，其余页面自动跳过（保持文件名高亮）
function syncNavByScroll() {
  const target = document.getElementById('contact');
  const homeLink = document.querySelector('.nav-links a[href="home.html"]');
  const contactLink = document.querySelector('.nav-links a[href="home.html#contact"]');
  if (!target || !homeLink || !contactLink) return; // 仅首页有锚点区块
  const top = target.getBoundingClientRect().top;
  const inView = top < window.innerHeight * 0.6; // 区块进入视口上部即切换
  contactLink.classList.toggle('active', inView);
  homeLink.classList.toggle('active', !inView);
}
let navScrollTicking = false;
window.addEventListener('scroll', function () {
  if (navScrollTicking) return;
  navScrollTicking = true;
  requestAnimationFrame(function () { navScrollTicking = false; syncNavByScroll(); });
});

// 点击页面其他区域关闭"更多"下拉
document.addEventListener('click', function (e) {
  if (!e.target.closest('.nav-more')) {
    const m = document.getElementById('navMore');
    if (m) m.classList.remove('open');
  }
});

// ===== 初始化：登录态渲染 + 导航高亮（DOM 就绪后执行）=====
function initPage() {
  renderLoginState();
  initNav();
  syncNavByScroll(); // 首帧对齐：URL 带 #contact 进入时浏览器已滚到锚点，滚动联动立即生效
  loadSiteConfig(); // 站点配置：footer/联系区动态展示（失败保留静态，无感）
}
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initPage);
} else {
  initPage();
}

// ===== 站点配置：导航/页脚/联系方式动态展示（第三批 + 站点级内容进后台）=====
// 后台「站点设置」改键，前台所有页面刷新即生效；配置为空/接口失败保留静态内容
const SITE_KEYS = ['site_phone', 'site_email', 'site_address', 'site_wechat',
  'site.nav', 'site.footer.about', 'site.footer.contact', 'site.footer.join'];

async function loadSiteConfig() {
  const cfg = {};
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 3000);
    await Promise.all(SITE_KEYS.map(async k => {
      try {
        const res = await fetch('/prod-api/system/config/configKey/' + k, { signal: ctrl.signal });
        const d = await res.json();
        // 接口返回 {code:200, msg:值}（无 data 字段）——此前读 d.data.configValue 永远取不到值，
        // 站点设置（导航/页脚/联系方式）被静默丢弃、前台一直显示静态内容
        if (d.code === 200 && d.msg) cfg[k] = String(d.msg).trim();
      } catch (e) { /* 单个键失败跳过 */ }
    }));
    clearTimeout(timer);
  } catch (e) { return; }
  if (!cfg.site_phone && !cfg.site_address && !cfg.site_wechat
    && !cfg['site.nav'] && !cfg['site.footer.about'] && !cfg['site.footer.contact'] && !cfg['site.footer.join']) return;

  // 1) footer 联系列（全站统一结构：找到 h4 含"联系"的列，整段替换 地址/电话/公众号）
  document.querySelectorAll('footer .footer-col').forEach(col => {
    const h = col.querySelector('h4');
    const p = col.querySelector('p');
    if (!h || !p) return;
    if ((h.textContent || '').indexOf('联系') === -1) return;
    const lines = [];
    if (cfg.site_address) lines.push('地址：' + cfg.site_address);
    if (cfg.site_phone) lines.push('电话：' + cfg.site_phone);
    if (cfg.site_wechat) lines.push('公众号：' + cfg.site_wechat);
    if (lines.length) p.innerHTML = lines.map(esc).join('<br>');
  });

  // 2) 首页联系区（.contact-item 逐项替换：地址/电话/公众号/视频号）
  document.querySelectorAll('.contact-list .contact-item').forEach(item => {
    const label = item.querySelector('.c-label');
    const value = item.querySelector('.c-value');
    if (!label || !value) return;
    const t = label.textContent || '';
    let v = null;
    if (t.indexOf('地址') !== -1) v = cfg.site_address;
    else if (t.indexOf('电话') !== -1) v = cfg.site_phone;
    else if (t.indexOf('公众号') !== -1) v = cfg.site_wechat ? cfg.site_wechat.split('｜')[0].trim() : null;
    else if (t.indexOf('视频号') !== -1) v = cfg.site_wechat && cfg.site_wechat.indexOf('｜') !== -1 ? cfg.site_wechat.split('｜')[1].trim() : null;
    if (v) value.textContent = v;
  });

  // 3) 导航菜单（site.nav JSON → #navAnchors；配置为空/损坏保留静态导航）
  if (cfg['site.nav']) {
    try {
      const items = JSON.parse(cfg['site.nav']);
      const box = document.getElementById('navAnchors');
      if (Array.isArray(items) && items.length && box) {
        const html = items.map(n =>
          '<a href="' + esc(safeLink(n.link)) + '">' + esc(n.name || '') + '</a>').join('');
        // 内容与现渲染一致时跳过 innerHTML 重写：省一次全量替换（切页卡顿主因）。
        // 比较时忽略 active 类——initNav 会在渲染后打上当前页高亮，读回值必带 class 差异
        if (box.innerHTML.replace(/\sclass="active"/g, '') !== html) {
          box.innerHTML = html;
          initNav(); // 重跑 URL 高亮（整体替换 innerHTML 会丢掉 active 类）
        }
        box.classList.remove('nav-pending'); // 配置已渲染：显示导航（同步块隐藏的在此恢复）
        // 写入带时间戳的本地缓存：60s 内切页同步渲染零闪烁；过期不渲染旧顺序
        try { localStorage.setItem('opc_site_nav', JSON.stringify({ t: Date.now(), items: items })); } catch (e) {}
      }
    } catch (e) { /* 导航配置损坏保留静态 */ }
  }

  // 4) 页脚三栏（site.footer.* 按 h4 匹配替换，白名单 HTML；优先级高于 site_* 拼接）
  const FOOTER_MAP = { 'site.footer.about': '关于我们', 'site.footer.contact': '联系我们', 'site.footer.join': '入驻与合作' };
  Object.keys(FOOTER_MAP).forEach(key => {
    if (!cfg[key]) return;
    document.querySelectorAll('footer .footer-col').forEach(col => {
      const h = col.querySelector('h4');
      const p = col.querySelector('p');
      if (h && p && (h.textContent || '').indexOf(FOOTER_MAP[key]) !== -1) {
        // 存储为纯文本换行（RuoYi XSS 过滤会剥离 HTML 标签），渲染时转 <br> 后白名单净化
        p.innerHTML = cmsSanitizeHtml(String(cfg[key]).replace(/\n/g, '<br>'));
      }
    });
  });
}

// ===== CMS 区块：前台文本槽渐进增强加载（第二批）=====
// 用法：页面声明 window.CMS_BLOCK_SLOTS = { blockKey: [ {field, sel, mode} ] }，
//       再调用 loadCmsBlocks(pageKey)。
//       field: title/subtitle/content；sel: CSS 选择器；mode: content 字段 'text'|'html'。
//       区块内容为空 / 接口失败 / 3s 超时 → 保留静态内容（前台永不白屏）。
const CMS_BLOCK_API = '/prod-api/system/cmsBlock/publicList?pageKey=';

// 白名单 HTML 净化（html 槽用；白名单外标签剥壳只留文本，防存储型 XSS）
const CMS_ALLOWED_TAGS = new Set(['P','BR','STRONG','B','EM','I','U','H1','H2','H3','H4','UL','OL','LI','A','IMG','BLOCKQUOTE','PRE','CODE','SPAN','TABLE','THEAD','TBODY','TR','TH','TD','DIV','HR']);

function cmsSanitizeHtml(html) {
  const doc = new DOMParser().parseFromString(String(html || ''), 'text/html');
  const out = document.createDocumentFragment();
  (function walk(node, frag) {
    Array.from(node.childNodes).forEach(child => {
      if (child.nodeType === 3) { frag.appendChild(document.createTextNode(child.nodeValue)); return; }
      if (child.nodeType !== 1) return;
      const tag = child.tagName;
      if (!CMS_ALLOWED_TAGS.has(tag)) { walk(child, frag); return; }
      const el = document.createElement(tag.toLowerCase());
      if (tag === 'A') {
        const href = child.getAttribute('href') || '';
        if (/^https?:\/\//i.test(href)) { el.href = href; el.target = '_blank'; el.rel = 'noopener'; }
      } else if (tag === 'IMG') {
        const src = child.getAttribute('src') || '';
        if (/^(https?:\/\/|\/)/i.test(src)) el.src = src;
        el.style.maxWidth = '100%';
      }
      walk(child, el);
      frag.appendChild(el);
    });
  })(doc.body, out);
  const tmp = document.createElement('div');
  tmp.appendChild(out);
  return tmp.innerHTML;
}

async function loadCmsBlocks(pageKey) {
  const slotMap = window.CMS_BLOCK_SLOTS || {};
  if (!Object.keys(slotMap).length) return;
  let data = [];
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 3000);
    const res = await fetch(CMS_BLOCK_API + pageKey, { signal: ctrl.signal });
    clearTimeout(timer);
    const d = await res.json();
    if (d.code === 200) data = d.data || [];
  } catch (e) { return; } // 失败/超时：保留静态内容
  data.forEach(b => {
    const slots = slotMap[b.blockKey];
    if (!slots) return;
    slots.forEach(s => {
      const el = document.querySelector(s.sel);
      if (!el) return;
      el.setAttribute('data-block-key', b.blockKey); // 预览标注/高亮定位用（无副作用）
      const val = b[s.field == null ? 'content' : s.field];
      if (val == null || String(val).trim() === '') return; // 留空 = 不覆盖静态内容
      if (s.field === 'content' && s.mode === 'html') {
        el.innerHTML = cmsSanitizeHtml(val);
      } else {
        el.textContent = val;
      }
    });
  });
  applyPreviewMarks();
}

// ===== 首页模块化渲染（内容引擎化：首页模块与栏目页区块统一由 cms_block 驱动，静态兜底）=====
const CMS_SECTION_API = '/prod-api/system/cmsSection/publicList?pageKey=home'; // 遗留接口（数据已迁入 cms_block，保留兼容）

function secHead(no, title, en) {
  return '<div class="home-mod-head"><span class="home-mod-no">' + (no < 10 ? '0' + no : no) + '</span>' +
    '<h2 class="home-mod-title">' + esc(title) + '</h2>' +
    (en ? '<span class="home-mod-en">' + esc(en) + '</span>' : '') + '</div>';
}

function renderSection(s, no) {
  let cfg = {};
  try { cfg = s.configJson ? JSON.parse(s.configJson) : {}; } catch (e) { cfg = {}; }
  // 背景色交替（品牌理念/三大赋能/产业生态/新闻/联系 依次轮换，避免同色堆叠）
  const BG_CYCLE = ['home-t1', 'home-t2', 'home-t3', 'home-t4', 'home-t1', 'home-t2', 'home-t3', 'home-t4'];
  const bg = BG_CYCLE[(no - 1) % BG_CYCLE.length];
  if (s.template === 'hero') {
    // 首屏文案配置驱动（页面搭建可改；无配置时用默认文案兜底）
    const hTitle = cfg.title || '清远市首个人工智能 OPC 生态社区';
    const hSub = cfg.subtitle || '一个人 + AI，把想法变成事业';
    const hText = cfg.content || '数智游民创新工场由清城区政府与清远星链科技合作共建，以"国企引领、民企赋能"模式运营，为 AI 时代的超级个体与一人公司提供"拎脑入驻"的创业生态。';
    return '<section id="home" class="home-hero">' +
      '<div class="home-hero-carousel"><div class="banner"><div id="bannerSlides"></div><div class="banner-dots" id="bannerDots"></div></div></div>' +
      '<div class="home-intro">' +
      '<h2 class="home-intro-title">' + esc(hTitle) + '</h2>' +
      '<p class="home-intro-sub">' + esc(hSub) + '</p>' +
      '<p class="home-intro-text">' + esc(hText) + '</p>' +
      '<div class="home-intro-btns"><a class="btn-buy" href="about.html" style="text-decoration:none">走进社区</a>' +
      '<a class="btn-buy home-btn-ghost" href="join.html" style="text-decoration:none">立即入驻</a></div>' +
      '<div class="home-scroll-hint">▼ 向下滚动探索</div></div></section>';
  }
  if (s.template === 'cards') {
    const cards = (cfg.cards || []).map(c =>
      '<div class="about-card"><div class="about-icon">' + esc(c.icon || '📄') + '</div>' +
      '<div class="about-title">' + esc(c.title) + '</div><p>' + esc(c.text) + '</p></div>').join('');
    // 可选步骤条（cfg.steps：{title, desc} 数组，样式复用 home-steps）
    const steps = (cfg.steps || []).map((st, si) =>
      '<div class="hs-item"><span class="hs-no">' + ['①', '②', '③', '④', '⑤'][si] + '</span>' +
      '<div><span class="hs-title">' + esc(st.title) + '</span><span class="hs-desc">' + esc(st.desc) + '</span></div></div>' +
      (si < cfg.steps.length - 1 ? '<div class="hs-arrow">→</div>' : '')).join('');
    return '<section class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title) +
      '<div class="about-cards" style="grid-template-columns:repeat(' + safeCols(cfg.cols) + ',1fr)">' + cards + '</div>' +
      (steps ? '<div class="home-steps">' + steps + '</div>' : '') + '</div></section>';
  }
  if (s.template === 'tags') {
    const groups = (cfg.groups || []).map(g =>
      '<div class="home-plus-tags"><span class="hpt-title">' + esc(g.title) + '：</span>' +
      (g.tags || []).map(t => '<span>' + esc(t) + '</span>').join('') + '</div>').join('');
    return '<section class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title) + groups + '</div></section>';
  }
  if (s.template === 'news') {
    // 渲染容器后自包含拉取新闻（动态模块不依赖 home.html 内联函数）
    const count = (cfg && cfg.count) || 6;
    setTimeout(async () => {
      const box = document.getElementById('newsList');
      if (!box) return;
      try {
        const ctrl = new AbortController();
        const timer = setTimeout(() => ctrl.abort(), 3000);
        const res = await fetch('/prod-api/system/cms/publicList?pageNum=1&pageSize=' + count, { signal: ctrl.signal });
        clearTimeout(timer);
        const d = await res.json();
        const rows = d.rows || [];
        if (!rows.length) { box.innerHTML = '<div class="no-result">暂无新闻</div>'; return; }
        box.innerHTML = rows.map(a => {
          const raw = String(a.summary || '').replace(/<[^>]+>/g, '');
          // 封面缩略图：统一左图右文结构——有封面显示真实图，无封面显示占位块（保证标题起点对齐）
          const cover = a.cover
            ? '<img class="news-thumb" src="' + esc(/^https?:\/\//.test(a.cover) ? a.cover : '/prod-api' + a.cover) + '" alt="" loading="lazy" onerror="this.style.display=\'none\'">'
            : '<div class="news-thumb news-thumb-ph">📰</div>';
          return '<div class="news-card has-thumb" onclick="location.href=\'article.html?id=' + a.articleId + '\'">' +
            cover +
            '<div class="news-body">' +
              '<div class="news-title">' + (a.isTop === '1' ? '<span class="tag tag-off" style="background:#fdf0ec;color:#e2554b">置顶</span> ' : '') + esc(a.title || '') + '</div>' +
              '<div class="news-date">🕐 ' + esc(a.publishTime || '') + ' ｜ ' + esc(a.categoryName || '') + '</div>' +
              '<div class="news-summary">' + esc(raw.slice(0, 120)) + (raw.length > 120 ? '…' : '') + '</div>' +
            '</div>' +
          '</div>';
        }).join('');
      } catch (e) {
        box.innerHTML = '<div class="no-result">新闻加载失败</div>';
      }
    }, 0);
    return '<section class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title, 'NEWS') +
      '<div id="newsList" class="news-list"><div class="loading">加载中…</div></div></div></section>';
  }
  if (s.template === 'timeline') {
    const items = (cfg.items || []).map(it =>
      '<div class="tl-item"><div class="tl-date">' + esc(it.date) + '</div>' +
      '<div class="tl-title">' + esc(it.title) + '</div><div class="tl-desc">' + esc(it.desc) + '</div></div>').join('');
    return '<section class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title) +
      '<div class="timeline">' + items + '</div></div></section>';
  }
  if (s.template === 'contact') {
    const items = (cfg.items || []).map(it =>
      '<div class="tl-item"><div class="tl-date">' + esc(it.date) + '</div>' +
      '<div class="tl-title">' + esc(it.title) + '</div><div class="tl-desc">' + esc(it.desc) + '</div></div>').join('');
    return '<section id="contact" class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title, 'MILESTONES & CONTACT') +
      '<div class="home-merge"><div class="timeline" style="flex:1.1;max-width:560px">' + items + '</div>' +
      '<div style="flex:1;max-width:420px"><div class="contact-list">' +
      '<div class="contact-item"><span class="c-icon">📍</span><div><div class="c-label">地址</div><div class="c-value">清远国家高新技术产业开发区天安智谷产业园 B6 栋、T1 栋 1105</div></div></div>' +
      '<div class="contact-item"><span class="c-icon">📞</span><div><div class="c-label">电话</div><div class="c-value">0763-3391888</div></div></div>' +
      '<div class="contact-item"><span class="c-icon">💬</span><div><div class="c-label">公众号</div><div class="c-value">互动世界</div></div></div>' +
      '<div class="contact-item"><span class="c-icon">📱</span><div><div class="c-label">视频号</div><div class="c-value">互动AI世界</div></div></div>' +
      '</div></div></div></div></section>';
  }
  if (s.template === 'cta' || s.template === 'banner_text') {
    const img = s.template === 'banner_text' && cfg.image
      ? '<img src="' + esc(/^https?:\/\//.test(cfg.image) ? cfg.image : '/prod-api' + cfg.image) + '" style="width:100%;max-height:220px;object-fit:cover;border-radius:10px;margin-bottom:14px" />' : '';
    return '<section class="home-cta"><div class="home-cta-inner">' + img +
      '<h3>' + esc(cfg.title || '') + '</h3>' +
      (cfg.text ? '<p>' + esc(cfg.text) + '</p>' : '') +
      (cfg.btnText ? '<a class="btn-buy home-cta-btn" href="' + esc(safeLink(cfg.btnLink)) + '">' + esc(cfg.btnText) + '</a>' : '') +
      '</div></section>';
  }
  // text：纯文本段落
  return '<section class="home-mod ' + bg + '"><div class="container">' + secHead(no, s.title) +
    '<div style="font-size:15px;line-height:2;color:var(--text)">' + esc(cfg.text || '') + '</div></div></section>';
}

/** 首页模块渲染（同步缓存渲染与异步拉取共用，保证两路输出一致） */
function renderHomeBlocks(list) {
  const mapped = list.map(b => ({ ...b, sectionKey: b.sectionKey || b.blockKey }));
  let no = 0;
  let html = mapped.map(s => { no += 1; return renderSection(s, no); }).join('');
  // 按渲染顺序给 section 打 data-section-key（预览标注/高亮定位用，正式页面无副作用）
  let si = 0;
  return html.replace(/<section\b[^>]*>/g, m => {
    const s = mapped[si++];
    if (!s) return m;
    return m.replace(/>\s*$/, ' data-section-key="' + esc(s.sectionKey || '') + '">');
  });
}

/** 恢复静态主体显示（无配置区块或请求失败时调用） */
function revealStatic(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('content-pending');
}

async function loadHomeSections() {
  const box = document.getElementById('homeSections');
  if (!box) return;
  // 先绑定静态模块吸附（兜底场景）；动态渲染成功后再重绑（幂等清理旧绑定）
  if (window.__initHomeAnimations) window.__initHomeAnimations();
  let list = [];
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 3000);
    // 内容引擎化：首页模块已并入 cms_block（pageKey=home，template 非空 = 模块）
    const res = await fetch(CMS_BLOCK_API + 'home', { signal: ctrl.signal });
    clearTimeout(timer);
    const d = await res.json();
    if (d.code === 200) list = (d.data || []).filter(b => b.template && b.template !== '' && b.visible === '0');
  } catch (e) { revealStatic('homeStatic'); return; } // 失败/超时：保留静态模块（吸附已绑定静态）
  if (!list.length) { revealStatic('homeStatic'); return; }
  const html = renderHomeBlocks(list);
  const cachedList = (() => { try { const c = JSON.parse(localStorage.getItem('opc_blocks_home')); return c && Array.isArray(c.list) ? c.list : null; } catch (e) { return null; } })();
  const stat = document.getElementById('homeStatic');
  const sameAsCached = cachedList && JSON.stringify(cachedList) === JSON.stringify(list);
  if (stat || !sameAsCached) {
    box.innerHTML = html;
    if (stat) stat.parentNode.removeChild(stat); // 移除静态兜底（区块槽位重新定位到动态元素）
    applyPreviewMarks();
  }
  // 动态 hero 轮播与滚动动画：无论是否重渲染都重绑（幂等；同步阶段内联脚本可能尚未定义）
  if (window.__loadBanners) window.__loadBanners();
  if (window.__initHomeAnimations) window.__initHomeAnimations();
  // 刷新缓存时间戳（无论是否重渲染）
  try { localStorage.setItem('opc_blocks_home', JSON.stringify({ t: Date.now(), list: list })); } catch (e) {}
}

// ===== 后台预览标注模式（配合后台页面搭建/区块管理的实时预览 iframe）=====
// 1) ?preview=1 时给带 data-section-key / data-block-key 的容器加虚线框 + 角标
// 2) ?highlight=key 或收到 postMessage 时高亮定位对应模块
function markPreviewEl(el, key) {
  el.classList.add('preview-mark');
  let tag = el.querySelector(':scope > .preview-tag');
  if (!tag) {
    tag = document.createElement('span');
    tag.className = 'preview-tag';
    el.appendChild(tag);
  }
  tag.textContent = key;
}
function applyPreviewMarks() {
  if (!window.IS_PREVIEW) return;
  document.querySelectorAll('[data-section-key]').forEach(el => markPreviewEl(el, el.dataset.sectionKey));
  document.querySelectorAll('[data-block-key]').forEach(el => markPreviewEl(el, el.dataset.blockKey));
  if (window.PREVIEW_HIGHLIGHT) highlightPreviewKey(window.PREVIEW_HIGHLIGHT);
}
function highlightPreviewKey(key) {
  if (!key) return;
  document.querySelectorAll('.preview-highlight').forEach(el => el.classList.remove('preview-highlight'));
  const el = document.querySelector('[data-section-key="' + key + '"], [data-block-key="' + key + '"]');
  if (el) {
    el.classList.add('preview-highlight');
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
window.addEventListener('message', function (e) {
  const d = e.data;
  if (!d || d.type !== 'opc-preview') return;
  if (d.action === 'scrollTo') highlightPreviewKey(d.key);
});

// ===== 栏目页内容区块渲染（区块管理 v3：栏目页主体模块化）=====
// 模板集（15 个）：text/feature/cards/steps/list/tags/timeline/stats/quote/cta/form/banner/faq/team/price
// 样式 .pblock 前缀（阅读型文档流：白卡/浅底 + 大间距 + 居中容器，与首页 renderSection 完全隔离）
// 区块标题 = 后台区块的 title 字段（内容区块必填）；正文/配置来自 config_json
function renderPageBlock(b, no) {
  let cfg = {};
  try { cfg = b.configJson ? JSON.parse(b.configJson) : {}; } catch (e) { cfg = {}; }
  const t = b.template;
  const key = b.blockKey || '';
  const inner = (body) => '<section class="pblock" data-section-key="' + esc(key) + '"><div class="pblock-inner">' + body + '</div></section>';
  const head = (title) => (title ? '<h2 class="pblock-title">' + esc(title) + '</h2>' : '');
  const imgUrl = (u) => (/^https?:\/\//.test(u || '') ? u : '/prod-api' + (u || ''));

  if (t === 'text') {
    return inner(head(b.title) +
      (cfg.subtitle ? '<div class="pblock-sub">' + esc(cfg.subtitle) + '</div>' : '') +
      '<div class="pblock-text">' + cmsSanitizeHtml(cfg.text || '') + '</div>');
  }
  if (t === 'feature') {
    const img = cfg.image ? '<div class="pblock-f-img"><img src="' + esc(imgUrl(cfg.image)) + '" alt="" /></div>' : '';
    return inner('<div class="pblock-feature' + (cfg.reverse === '1' ? ' pblock-feature-rev' : '') + '">' + img +
      '<div class="pblock-f-body">' + head(b.title) +
      (cfg.text ? '<div class="pblock-text">' + cmsSanitizeHtml(cfg.text) + '</div>' : '') +
      (cfg.btnText ? '<a class="btn-buy pblock-f-btn" href="' + esc(safeLink(cfg.btnLink)) + '" style="text-decoration:none">' + esc(cfg.btnText) + '</a>' : '') +
      '</div></div>');
  }
  if (t === 'cards') {
    const cards = (cfg.cards || []).map(c =>
      '<div class="pblock-card"><div class="pblock-card-icon">' + esc(c.icon || '📄') + '</div>' +
      '<div class="pblock-card-title">' + esc(c.title) + '</div>' + cmsSanitizeHtml(c.text || '') + '</div>').join('');
    return inner(head(b.title) +
      (cfg.subtitle ? '<div class="pblock-sub">' + esc(cfg.subtitle) + '</div>' : '') +
      '<div class="pblock-cards" style="grid-template-columns:repeat(' + safeCols(cfg.cols) + ',1fr)">' + cards + '</div>');
  }
  if (t === 'steps') {
    const steps = (cfg.steps || []).map((st, i) =>
      '<div class="pblock-step"><span class="pblock-step-no">' + ['①', '②', '③', '④', '⑤', '⑥'][i] + '</span>' +
      '<div class="pblock-step-body"><b>' + esc(st.title) + '</b><p>' + esc(st.desc) + '</p></div></div>').join('');
    return inner(head(b.title) + '<div class="pblock-steps">' + steps + '</div>');
  }
  if (t === 'list') {
    const items = (cfg.items || []).map(it =>
      '<li><b>' + esc(it.title) + '</b>' + (it.desc ? '<span>' + esc(it.desc) + '</span>' : '') + '</li>').join('');
    return inner(head(b.title) + '<ul class="pblock-list">' + items + '</ul>');
  }
  if (t === 'tags') {
    const groups = (cfg.groups || []).map(g =>
      '<div class="pblock-tags-row"><span class="pblock-tags-label">' + esc(g.title) + '：</span>' +
      (g.tags || []).map(tg => '<span class="pblock-tag">' + esc(tg) + '</span>').join('') + '</div>').join('');
    return inner(head(b.title) + '<div class="pblock-tags">' + groups + '</div>');
  }
  if (t === 'timeline') {
    const items = (cfg.items || []).map(it =>
      '<div class="pblock-tl-item"><div class="pblock-tl-date">' + esc(it.date) + '</div>' +
      '<div class="pblock-tl-body"><b>' + esc(it.title) + '</b><p>' + esc(it.desc) + '</p></div></div>').join('');
    return inner(head(b.title) + '<div class="pblock-tl">' + items + '</div>');
  }
  if (t === 'stats') {
    const stats = (cfg.items || []).map(st =>
      '<div class="pblock-stat"><div class="pblock-stat-num">' + esc(st.value) + '</div>' +
      '<div class="pblock-stat-label">' + esc(st.label) + '</div>' +
      (st.desc ? '<div class="pblock-stat-desc">' + esc(st.desc) + '</div>' : '') + '</div>').join('');
    return inner(head(b.title) + '<div class="pblock-stats">' + stats + '</div>' +
      (cfg.text ? '<div class="pblock-text" style="margin-top:18px">' + cmsSanitizeHtml(cfg.text) + '</div>' : ''));
  }
  if (t === 'quote') {
    return inner('<div class="pblock-quote"><div class="pblock-quote-text">' + esc(cfg.text || '') + '</div>' +
      (cfg.author ? '<div class="pblock-quote-author">—— ' + esc(cfg.author) + '</div>' : '') + '</div>');
  }
  if (t === 'banner') {
    // 大图横幅：背景图 + 标题 + 正文 + 按钮；无图时用渐变底色（样式 .pblock-banner-grad）
    const bg = cfg.image ? ' style="background-image:url(' + esc(imgUrl(cfg.image)) + ')"' : '';
    return '<section class="pblock pblock-banner' + (cfg.image ? '' : ' pblock-banner-grad') + '" data-section-key="' + esc(key) + '"' + bg + '><div class="pblock-banner-inner">' +
      '<h3>' + esc(cfg.title || b.title || '') + '</h3>' +
      (cfg.text ? '<p>' + esc(cfg.text) + '</p>' : '') +
      (cfg.btnText ? '<a class="btn-buy pblock-banner-btn" href="' + esc(safeLink(cfg.btnLink)) + '" style="text-decoration:none">' + esc(cfg.btnText) + '</a>' : '') +
      '</div></section>';
  }
  if (t === 'faq') {
    // 常见问题：details/summary 纯 CSS 折叠，无需 JS
    const items = (cfg.items || []).map(it =>
      '<details class="pblock-faq-item"><summary>' + esc(it.q || '') + '</summary>' +
      '<div class="pblock-faq-a">' + esc(it.a || '') + '</div></details>').join('');
    return inner(head(b.title) + '<div class="pblock-faq">' + items + '</div>');
  }
  if (t === 'team') {
    // 成员/企业卡片：头像（可留空用默认）+ 姓名 + 身份 + 简介
    const cards = (cfg.items || []).map(c =>
      '<div class="pblock-team-card">' +
      (c.image ? '<div class="pblock-team-avatar"><img src="' + esc(imgUrl(c.image)) + '" alt="" /></div>' : '<div class="pblock-team-avatar pblock-team-avatar-empty">👤</div>') +
      '<div class="pblock-team-name">' + esc(c.name || '') + '</div>' +
      (c.role ? '<div class="pblock-team-role">' + esc(c.role) + '</div>' : '') +
      (c.desc ? '<div class="pblock-team-desc">' + esc(c.desc) + '</div>' : '') + '</div>').join('');
    return inner(head(b.title) +
      '<div class="pblock-team" style="grid-template-columns:repeat(' + safeCols(cfg.cols) + ',1fr)">' + cards + '</div>');
  }
  if (t === 'price') {
    // 费用/权益表：项目 + 费用 + 说明 三列表格
    const rows = (cfg.items || []).map(it =>
      '<tr><td class="pblock-price-name">' + esc(it.name || '') + '</td><td class="pblock-price-val">' + esc(it.price || '') + '</td>' +
      '<td>' + esc(it.desc || '') + '</td></tr>').join('');
    return inner(head(b.title) + '<div class="pblock-table-wrap"><table class="pblock-price"><tbody>' + rows + '</tbody></table></div>');
  }
  if (t === 'cta') {
    return '<section class="pblock pblock-cta" data-section-key="' + esc(key) + '"><div class="pblock-cta-inner">' +
      '<h3>' + esc(cfg.title || b.title || '') + '</h3>' +
      (cfg.text ? '<p>' + esc(cfg.text) + '</p>' : '') +
      (cfg.btnText ? '<a class="btn-buy" href="' + esc(safeLink(cfg.btnLink)) + '" style="text-decoration:none">' + esc(cfg.btnText) + '</a>' : '') +
      '</div></section>';
  }
  if (t === 'form') {
    // 入驻申请表单（join 页专用；submitJoin() 按 id 取元素，模板渲染后功能不变）
    return inner(head(b.title) +
      '<div class="form-card">' +
      '<div class="form-item"><label>项目/组织名称 <span class="req">*</span></label><input id="joinName" class="form-input" placeholder="如：某某 AI 内容工作室" /></div>' +
      '<div class="form-item"><label>联系人 <span class="req">*</span></label><input id="joinAuthor" class="form-input" placeholder="✍️ 联系人姓名" /></div>' +
      '<div class="form-item"><label>联系邮箱 <span class="req">*</span></label><input id="joinEmail" class="form-input" placeholder="📧 处理结果将通过邮件通知" /></div>' +
      '<div class="form-item"><label>申请说明（选填）</label><textarea id="joinRemark" class="form-input" rows="3" placeholder="需要的服务 / 意向入驻类型（A 类免费合伙人 / B 类付费成员）/ 项目简介"></textarea></div>' +
      '<button class="btn-buy" onclick="submitJoin()" style="font-size:15px;padding:11px 34px">提交入驻申请</button>' +
      '<div id="joinMsg" class="form-msg"></div>' +
      '<p style="font-size:12px;color:#8a8a8a;margin-top:10px">提交即表示同意社区入驻规则，运营团队将在收到申请后尽快与您联系。</p>' +
      '</div>');
  }
  // 兜底：未知模板按文本段落渲染
  return inner(head(b.title) + '<div class="pblock-text">' + esc(cfg.text || b.content || '') + '</div>');
}

/** 栏目页主体模块化渲染：内容区块列表 → #pageSections；接口失败/无内容区块 → 保留静态主体兜底 */
async function loadPageSections(pageKey) {
  const box = document.getElementById('pageSections');
  if (!box) return;
  let list = [];
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 3000);
    const res = await fetch(CMS_BLOCK_API + pageKey, { signal: ctrl.signal });
    clearTimeout(timer);
    const d = await res.json();
    if (d.code === 200) list = (d.data || []).filter(b => b.template && b.template !== '' && b.visible === '0');
  } catch (e) { revealStatic('pageStatic'); return; }
  if (!list.length) { revealStatic('pageStatic'); return; } // 保留静态主体（页面永不白屏）
  const cachedList = (() => { try { const c = JSON.parse(localStorage.getItem('opc_blocks_' + pageKey)); return c && Array.isArray(c.list) ? c.list : null; } catch (e) { return null; } })();
  const stat = document.getElementById('pageStatic');
  const sameAsCached = cachedList && JSON.stringify(cachedList) === JSON.stringify(list);
  if (stat || !sameAsCached) {
    // 同步块未渲染（静态还在）或后台内容有变：执行渲染；
    // 内容一致且同步块已渲染：跳过全量 innerHTML（切页卡顿主因）
    box.innerHTML = list.map((b, i) => renderPageBlock(b, i + 1)).join('');
    if (stat) stat.parentNode.removeChild(stat); // 移除静态兜底主体
    applyPreviewMarks();
  }
  // 刷新缓存时间戳（无论是否重渲染：保持"新鲜"窗口，下次切页走同步渲染）
  try { localStorage.setItem('opc_blocks_' + pageKey, JSON.stringify({ t: Date.now(), list: list })); } catch (e) {}
}

// ===== 区块内容防闪烁（区块管理保存的内容切页即显，不闪静态原版）=====
// 必须放在脚本尾部：渲染器引用 const（CMS_ALLOWED_TAGS 等），TDZ 下前置调用会抛
// ReferenceError 并中断整段脚本（曾因此导致高亮/后续初始化全部失效）。
// ① 新鲜缓存（≤60s）→ 同步渲染区块并移除静态主体（仍在首绘前，切页零闪烁）
// ② 缓存缺失/过期 → 先隐藏静态主体（.content-pending，不占位抖动），异步配置到达后显示
// ③ 请求失败/无区块 → 异步路径或 3.5s 兜底恢复静态主体（页面永不空白）
(function () {
  const PAGE_KEYS = { 'home.html': 'home', 'about.html': 'about', 'join.html': 'join', 'talent.html': 'talent', 'industry.html': 'industry' };
  const page = (location.pathname.split('/').pop() || 'home.html').split('?')[0];
  const pageKey = PAGE_KEYS[page] || '';
  if (!pageKey) return;
  const isHome = pageKey === 'home';
  const box = document.getElementById(isHome ? 'homeSections' : 'pageSections');
  if (!box) return;
  const statId = isHome ? 'homeStatic' : 'pageStatic';
  const stat = document.getElementById(statId);
  let list = null;
  try {
    const raw = localStorage.getItem('opc_blocks_' + pageKey);
    if (raw) {
      const c = JSON.parse(raw);
      if (c && Array.isArray(c.list) && c.list.length && Date.now() - (c.t || 0) <= 60000) {
        list = c.list; // 仅新鲜缓存做首绘前同步渲染；过期缓存不渲染（避免闪旧内容）
      }
    }
  } catch (e) { list = null; }
  if (list) {
    try {
      if (isHome) {
        box.innerHTML = renderHomeBlocks(list);
        // 首页动态 hero 轮播与滚动动画：同步阶段内联脚本可能尚未定义，异步路径会再重绑
        if (window.__loadBanners) window.__loadBanners();
        if (window.__initHomeAnimations) window.__initHomeAnimations();
      } else {
        box.innerHTML = list.map((b, i) => renderPageBlock(b, i + 1)).join('');
      }
      if (stat) stat.parentNode.removeChild(stat); // 移除静态兜底主体（与异步路径一致）
      if (typeof applyPreviewMarks === 'function') applyPreviewMarks();
    } catch (e) {
      // 渲染异常：不阻塞页面，恢复静态主体
      if (stat) stat.classList.remove('content-pending');
    }
  } else {
    if (stat) stat.classList.add('content-pending'); // 无新鲜缓存：先隐藏静态主体
    setTimeout(function () { revealStatic(statId); }, 3500); // 兜底：请求失败恢复静态
  }
})();
