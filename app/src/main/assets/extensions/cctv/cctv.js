// 央视/央视频自动全屏内容脚本（GeckoView 内置 WebExtension）。逻辑等价于系统 WebView 的
// CctvAutomation.pageScript：边精简页面、边轮询全屏按钮，出现即点一次进入网页全屏，并保活音量/播放。
(function () {
  if (window.__cctvAuto) return;
  window.__cctvAuto = true;
  var clicked = false, n = 0;

  // Gecko 内容脚本里改 viewport meta 不生效（实测 innerWidth 不变），改用 CSS 直接把
  // <video> 钉成铺满视口、固定不动，并禁掉页面滚动——绕开桌面页固定宽布局导致的溢出可拖动。
  function ensureCss() {
    try {
      if (document.getElementById('__cctvCss')) return;
      var s = document.createElement('style');
      s.id = '__cctvCss';
      s.textContent =
        'html,body{overflow:hidden!important;margin:0!important;width:100%!important;height:100%!important}' +
        'video{position:fixed!important;top:0!important;left:0!important;right:0!important;bottom:0!important;' +
        'width:100vw!important;height:100vh!important;max-width:none!important;max-height:none!important;' +
        'object-fit:contain!important;z-index:2147483647!important;background:#000!important;margin:0!important}';
      (document.head || document.documentElement).appendChild(s);
    } catch (e) {}
  }

  function strip() {
    try {
      Array.prototype.forEach.call(document.images, function (img) { img.src = ''; });
      var kw = ['login', 'index', 'daohang', 'grey', 'jquery'];
      Array.prototype.forEach.call(document.scripts, function (s) {
        if (s.src && kw.some(function (k) { return s.src.indexOf(k) !== -1; })) { s.src = ''; }
      });
      ['newmap', 'newtopbz', 'newtopbzTV', 'column_wrapper'].forEach(function (c) {
        Array.prototype.forEach.call(document.getElementsByClassName(c), function (d) { d.innerHTML = ''; });
      });
    } catch (e) {}
  }

  function tick() {
    ensureCss();
    var v = document.querySelector('video');
    if (v) {
      v.muted = false;
      try { v.volume = 1; } catch (e) {}
      if (v.paused) { try { v.play(); } catch (e) {} }
    }
    if (!clicked) {
      strip();
      var btn = document.querySelector('#player_pagefullscreen_yes_player')
             || document.querySelector('.videoFull');
      if (btn) { btn.click(); clicked = true; }
    }
    if (++n < 1200) { setTimeout(tick, clicked ? 500 : 16); }
  }

  tick();
})();
