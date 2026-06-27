package com.mytv.live

import android.app.Application
import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

/**
 * 全局 Application。
 *
 * GeckoRuntime（GeckoView 引擎）按需**懒加载**且为**进程级单例**（GeckoRuntime 一个进程只能创建一次）：
 * 只有真正使用 GeckoView 引擎（[com.mytv.live.web.GeckoWebPlayer]）时才创建，系统 WebView 模式下完全不拉起 Gecko。
 */
class MyTvApp : Application() {

    companion object {
        private const val TAG = "MyTvApp"

        @Volatile
        private var runtimeInstance: GeckoRuntime? = null

        /** 懒加载、进程唯一的 GeckoRuntime。首次访问（GeckoWebPlayer open session）时创建一次。 */
        fun geckoRuntime(context: Context): GeckoRuntime =
            runtimeInstance ?: synchronized(this) {
                runtimeInstance ?: createRuntime(context.applicationContext).also { runtimeInstance = it }
            }

        private fun createRuntime(app: Context): GeckoRuntime {
            val settings = GeckoRuntimeSettings.Builder()
                .remoteDebuggingEnabled(true)   // 桌面 Firefox about:debugging 可连上排查
                .consoleOutput(true)
                .aboutConfigEnabled(true)
                .configFilePath(writeGeckoConfig(app).absolutePath)  // 固化 pref（见下）
                .build()

            val rt = GeckoRuntime.create(app, settings)

            // 安装内置内容脚本扩展：自动点全屏 + 开声音 + 精简页面 + CSS 钉 video 铺满。
            rt.webExtensionController
                .installBuiltIn("resource://android/assets/extensions/cctv/")
                .accept(
                    { ext -> Log.i(TAG, "cctv extension installed: ${ext?.id}") },
                    { e -> Log.e(TAG, "cctv extension install failed", e) },
                )

            return rt
        }

        /**
         * 写出 GeckoView 配置文件并返回其路径，供 [GeckoRuntimeSettings.Builder.configFilePath] 加载。
         *
         * 固化两个 pref 放行自动播放：
         * - `media.geckoview.autoplay.request=false`：关掉 GeckoView 把自动播放走自家权限 API 的默认行为，
         *   否则它会盖掉下面的 `media.autoplay.default`（这也是为什么单设 default=0 无效）。
         * - `media.autoplay.default=0`：自动播放默认=允许。
         *
         * 背景：某些 Google TV 盒子上 PermissionDelegate 放行自动播放不生效，页面播放器调 `play()` 直接
         * 抛 `NotAllowedError` → "无法播放"。靠这两个 pref 从引擎层彻底放行，对所有设备无害。
         * 详见 doc/排查记录-某盒子无法播放央视.md。
         */
        private fun writeGeckoConfig(app: Context): File {
            val file = File(app.filesDir, "geckoview-config.yaml")
            val yaml = buildString {
                appendLine("prefs:")
                appendLine("  media.geckoview.autoplay.request: false")
                appendLine("  media.autoplay.default: 0")
            }
            runCatching { file.writeText(yaml) }
                .onFailure { Log.e(TAG, "write geckoview-config failed", it) }
            return file
        }
    }
}
