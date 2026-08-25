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

// ===== 站点配置：联系方式动态展示（第三批）=====
// 后台「系统设置 → 参数设置」改 site_* 键，前台所有页面刷新即生效；配置为空/接口失败保留静态内容
const SITE_KEYS = ['site_phone', 'site_email', 'site_address', 'site_wechat'];

async function loadSiteConfig() {
  const cfg = {};
  try {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 3000);
    await Promise.all(SITE_KEYS.map(async k => {
      try {
        const res = await fetch('/prod-api/system/config/configKey/' + k, { signal: ctrl.signal });
        const d = await res.json();
        if (d.code === 200 && d.data && d.data.configValue) cfg[k] = String(d.data.configValue).trim();
      } catch (e) { /* 单个键失败跳过 */ }
    }));
    clearTimeout(timer);
  } catch (e) { return; }
  if (!cfg.site_phone && !cfg.site_address && !cfg.site_wechat) return;

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
      const val = b[s.field == null ? 'content' : s.field];
      if (val == null || String(val).trim() === '') return; // 留空 = 不覆盖静态内容
      if (s.field === 'content' && s.mode === 'html') {
        el.innerHTML = cmsSanitizeHtml(val);
      } else {
        el.textContent = val;
      }
    });
  });
}
