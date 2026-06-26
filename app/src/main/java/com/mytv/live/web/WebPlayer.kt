package com.mytv.live.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mytv.live.data.Channel

/**
 * 浏览器引擎类型 —— 套壳 App 的核心可替换层。
 *
 * 第一阶段只实现 [SYSTEM_WEBVIEW]（android.webkit.WebView），它自带 `evaluateJavascript`，
 * 可直接复用 ../CCTV_Viewer 的成熟注入逻辑，最快跑通。
 *
 * 第二阶段若盒子表现不一致或要更稳的 DRM，可加 [GECKOVIEW] 实现（Firefox 引擎打包进 APK，
 * 注入改走 WebExtension 内容脚本，复用 [CctvAutomation] 里的脚本字符串），切换只需改这里。
 */
enum class WebEngine { SYSTEM_WEBVIEW, GECKOVIEW }

/** 当前启用的引擎。要切回系统 WebView，把这里改成 [WebEngine.SYSTEM_WEBVIEW] 即可（一行）。 */
val ACTIVE_ENGINE: WebEngine = WebEngine.GECKOVIEW

/**
 * 套壳播放器入口。根据 [engine] 选择具体引擎实现，对上层（PlayerScreen）屏蔽差异。
 *
 * @param channel 当前频道（含 URL 与播放器类型）
 * @param onCanGoBackChanged 网页是否可后退的回调，供返回键决定行为
 * @param onBackingStore 把内部的"网页后退"动作暴露给上层（返回键触发）
 */
@Composable
fun WebPlayer(
    channel: Channel,
    modifier: Modifier = Modifier,
    engine: WebEngine = ACTIVE_ENGINE,
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onBackHandlerReady: (back: () -> Unit) -> Unit = {},
) {
    when (engine) {
        WebEngine.SYSTEM_WEBVIEW -> SystemWebPlayer(
            channel = channel,
            modifier = modifier,
            onCanGoBackChanged = onCanGoBackChanged,
            onBackHandlerReady = onBackHandlerReady,
        )
        WebEngine.GECKOVIEW -> GeckoWebPlayer(
            channel = channel,
            modifier = modifier,
            onCanGoBackChanged = onCanGoBackChanged,
            onBackHandlerReady = onBackHandlerReady,
        )
    }
}
