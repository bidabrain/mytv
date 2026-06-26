package com.mytv.live.web

import com.mytv.live.data.PlayerKind

/**
 * 页面自动化脚本 —— 移植自 ../CCTV_Viewer 中已验证可用的注入逻辑。
 *
 * 设计要点：
 * - 选台**不靠模拟鼠标点击**，而是每个频道直接加载各自 URL（见 [com.mytv.live.data.Channels]）。
 * - 进页面后唯一需要"点"的是播放器的全屏按钮：用 JS 轮询等它出现再 `.click()`，
 *   同时开声音、保活播放。央视官网用 `#player_pagefullscreen_yes_player`，央视频用 `.videoFull`。
 * - 整段合并为**一个幂等轮询脚本**（[pageScript]），在 onPageStarted 就启动，
 *   因此不依赖 onPageFinished 的触发时机（盒子上重型页面常偏晚/不稳）。
 *   `window.__cctvAuto` 保证同一文档只跑一个轮询；切台会 loadUrl 换新文档自动重置。
 *
 * 这些脚本以纯字符串形式集中存放，方便将来切到 GeckoView 时改写成 WebExtension 内容脚本复用。
 */
object CctvAutomation {

    /**
     * 合并脚本：边精简页面、边轮询全屏按钮，出现即点一次进入网页全屏，并持续保活音量/播放。
     *
     * - 进入网页全屏后 `#player_pagefullscreen_yes_player`（"进入"按钮）会消失，
     *   `clicked` 也已置位，故不会被重复点（避免点成"退出全屏"）。
     * - 命中前每 16ms 轮询，命中后降到 500ms 只做音量/播放保活，约 10 分钟后停。
     */
    val pageScript: String = """
        (function () {
          if (window.__cctvAuto) return;
          window.__cctvAuto = true;
          var clicked = false, n = 0;

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
    """.trimIndent()

    /** 兼容旧调用点：onPageStarted 用同一脚本即可（幂等）。 */
    val fastLoadingJs: String get() = pageScript

    /** 兼容旧调用点：onPageFinished 再注入一次也是幂等 no-op（守卫已置位）。 */
    fun fullscreenJsFor(@Suppress("UNUSED_PARAMETER") kind: PlayerKind): String = pageScript
}
