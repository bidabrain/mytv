package com.mytv.live

import android.app.Application
import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * 全局 Application。
 *
 * GeckoRuntime（GeckoView 引擎）按需**懒加载**且为**进程级单例**（GeckoRuntime 一个进程只能创建一次）：
 * 只有真正使用 GeckoView 引擎时才创建，系统 WebView 模式下完全不拉起 Gecko。
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
                .remoteDebuggingEnabled(true)
                .consoleOutput(true)
                .aboutConfigEnabled(true)
                .build()

            val rt = GeckoRuntime.create(app, settings)

            rt.webExtensionController
                .installBuiltIn("resource://android/assets/extensions/cctv/")
                .accept(
                    { ext -> Log.i(TAG, "cctv extension installed: ${ext?.id}") },
                    { e -> Log.e(TAG, "cctv extension install failed", e) },
                )

            return rt
        }
    }
}
