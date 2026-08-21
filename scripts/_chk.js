
  // ===== 共享：页脚年份 =====
  (function () {
    var el = document.getElementById('footerYear');
    if (el) el.textContent = new Date().getFullYear();
  })();
