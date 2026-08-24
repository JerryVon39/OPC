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
async function submitLogin() {
  const card = document.getElementById('loginCard').value.trim();
  const pwd = document.getElementById('loginPwd').value;
  const msg = document.getElementById('loginMsg');
  if (!card || !pwd) { msg.textContent = '请输入成员编号/手机号/邮箱和密码'; msg.style.color = '#c65d43'; return; }
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
  if (!name || !phone || !type) { msg.textContent = '请填写姓名、手机号和成员类型'; msg.style.color = '#c65d43'; return; }
  if (!email || !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(email)) { msg.textContent = '请填写有效的电子邮箱（用于验证与通知）'; msg.style.color = '#c65d43'; return; }
  if (!pwd || pwd !== pwd2) { msg.textContent = '两次输入的密码不一致'; msg.style.color = '#c65d43'; return; }
  if (pwd.length < 10) { msg.textContent = '密码长度至少 10 位，且含大小写/数字/符号至少 3 类'; msg.style.color = '#c65d43'; return; }
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
  if (pwd.length < 10) { msg.textContent = '密码长度至少 10 位'; msg.style.color = '#c65d43'; return; }
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
  if (!phone) { msg.textContent = '请输入新手机号'; msg.style.color = '#c65d43'; return; }
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

// ===== 初始化：加载登录态并渲染（DOM 就绪后执行）=====
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', renderLoginState);
} else {
  renderLoginState();
}
