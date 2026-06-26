package com.mytv.live.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mytv.live.data.Channel

/** 桌面 Chrome UA。yangshipin.cn 地方台需要桌面 UA 才会渲染播放器（移植自 ../CCTV_Viewer）。 */
internal const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

/**
 * 基于系统 WebView 的套壳播放器实现。
 *
 * 关键点：
 * - `mediaPlaybackRequiresUserGesture = false` 允许网页自动播放；
 * - onPageStarted 注入 [CctvAutomation.fastLoadingJs] 精简页面、加速；
 * - onPageFinished 注入 [CctvAutomation.autoFullscreenJs] 自动点全屏 + 开声音；
 * - 自定义 [WebChromeClient] 处理 HTML5 真全屏（onShowCustomView），兜底铺满整屏。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SystemWebPlayer(
    channel: Channel,
    modifier: Modifier = Modifier,
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onBackHandlerReady: (back: () -> Unit) -> Unit = {},
) {
    // 同一个 WebView 复用，避免每次切台重建。
    val webView = remember { mutableHolder<WebView>() }

    DisposableEffect(Unit) {
        onDispose {
            webView.value?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webView.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // 开远程调试：盒子上接 adb 后，桌面 Chrome chrome://inspect 可看真实 DOM/控制台，
            // 排查全屏按钮选择器/播放问题。个人自用，常开无妨。
            WebView.setWebContentsDebuggingEnabled(true)

            // 用一个 FrameLayout 容器，方便 WebChromeClient 把全屏视频的 customView 叠上来。
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.BLACK)
            }

            val wv = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false   // 允许自动播放
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    javaScriptCanOpenWindowsAutomatically = true
                    // 关掉宽视口/概览缩放与用户缩放：桌面页配宽视口会按桌面宽度铺开，
                    // 网页全屏后视频比屏幕大、可手指拖动。改用下面的 setInitialScale 适配。
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    @Suppress("DEPRECATION")
                    setSupportZoom(false)
                    builtInZoomControls = false
                    // 强制桌面 Chrome UA：央视频 yangshipin.cn 的地方台桌面页
                    // 在移动 UA 下会跳移动版/引导下载、播放器不渲染，地方台因此黑屏。
                    // 与 ../CCTV_Viewer 一致，央视台 tv.cctv.com 也兼容此 UA。
                    userAgentString = DESKTOP_USER_AGENT
                }

                // 把网页全屏（按 1920×1080 设计）整体缩放到适配本机屏幕，避免溢出/可拖动。
                // 播放恒为横屏，故按长边=宽、短边=高计算，与朝向无关（移植自 ../CCTV_Viewer）。
                val dm = context.resources.displayMetrics
                val longEdge = maxOf(dm.widthPixels, dm.heightPixels).toDouble()
                val shortEdge = minOf(dm.widthPixels, dm.heightPixels).toDouble()
                val scale = Math.round(minOf(longEdge / 1920.0, shortEdge / 1080.0) * 100).toInt()
                setInitialScale(scale.coerceAtLeast(1))

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (url != null && url != "about:blank") {
                            view.evaluateJavascript(CctvAutomation.fastLoadingJs, null)
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null && url != "about:blank") {
                            view.evaluateJavascript(CctvAutomation.fullscreenJsFor(channel.kind), null)
                        }
                        onCanGoBackChanged(view.canGoBack())
                    }
                }

                // 处理网页发起的真·全屏（HTML5 requestFullscreen），把视频铺满整屏。
                webChromeClient = object : WebChromeClient() {
                    private var customView: View? = null

                    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                        if (customView != null) {
                            callback.onCustomViewHidden()
                            return
                        }
                        customView = view
                        container.addView(
                            view,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    }

                    override fun onHideCustomView() {
                        customView?.let { container.removeView(it) }
                        customView = null
                    }
                }
            }

            container.addView(wv)
            webView.value = wv

            // 把"网页后退"动作交给上层返回键调用。
            onBackHandlerReady {
                if (wv.canGoBack()) wv.goBack()
            }

            wv.loadUrl(channel.url)
            container
        },
        update = {
            val wv = webView.value ?: return@AndroidView
            // 切台：当目标 URL 与当前不同才重载，避免重复加载。
            if (wv.url == null || !sameChannelUrl(wv.url, channel.url)) {
                wv.loadUrl(channel.url)
            }
        },
    )
}

private fun sameChannelUrl(current: String?, target: String): Boolean {
    if (current == null) return false
    // 页面加载后 url 可能被重定向/补全，做个宽松的前缀比对即可。
    return current.startsWith(target) || target.startsWith(current)
}

/** 一个可变持有器，避免在 Composable 里直接用 var 捕获带来的可空判断噪音。 */
private class Holder<T> { var value: T? = null }

private fun <T> mutableHolder() = Holder<T>()
