package com.mytv.live.web

import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mytv.live.MyTvApp
import com.mytv.live.data.Channel
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

private const val TAG = "GeckoWebPlayer"

/**
 * 基于 GeckoView（Firefox 引擎）的套壳播放器实现。
 *
 * 关键点：
 * - 复用 [MyTvApp.geckoRuntime]（进程唯一 GeckoRuntime）open session；
 * - 桌面 UA + 移动视口：地方台桌面页需桌面 UA 才渲染播放器，视口 MOBILE 让 Gecko 缩放适配；
 * - 自动全屏/开声靠内置内容脚本（assets/extensions/cctv）；自动播放/DRM 靠 PermissionDelegate；
 * - **ContentDelegate.onCrash/onKill**：内容进程崩溃/被系统杀时自动重开 session 并重载，避免黑屏/连累整体。
 */
@Composable
fun GeckoWebPlayer(
    channel: Channel,
    modifier: Modifier = Modifier,
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onBackHandlerReady: (back: () -> Unit) -> Unit = {},
) {
    val holder = remember { GeckoHolder() }

    DisposableEffect(Unit) {
        onDispose {
            holder.session?.apply {
                setActive(false)
                close()
            }
            holder.session = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = GeckoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.BLACK)
            }

            val settings = GeckoSessionSettings.Builder()
                .usePrivateMode(false)
                // 只覆盖 UA 字符串（yangshipin 认桌面 UA 才给桌面播放器），但 userAgentMode 保持
                // 移动 + viewportMode=MOBILE：否则"桌面模式"渲染会关掉移动视口缩放，导致网页全屏
                // 后画面比屏幕大、可拖动。两者配合让 Gecko 把页面缩放到屏宽。
                .userAgentOverride(DESKTOP_USER_AGENT)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .build()

            val session = GeckoSession(settings)

            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                    onCanGoBackChanged(canGoBack)
                }
            }

            // 内容进程崩溃/被系统杀掉时，重开 session 并重载当前频道，避免黑屏或拖垮主进程。
            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onCrash(s: GeckoSession) {
                    Log.e(TAG, "content process crashed; reopening session")
                    recover(s, context)
                }

                override fun onKill(s: GeckoSession) {
                    Log.e(TAG, "content process killed by system; reopening session")
                    recover(s, context)
                }

                private fun recover(s: GeckoSession, ctx: android.content.Context) {
                    runCatching {
                        if (!s.isOpen) s.open(MyTvApp.geckoRuntime(ctx))
                        holder.loadedUrl?.let { s.loadUri(it) }
                    }.onFailure { Log.e(TAG, "recover failed", it) }
                }
            }

            // 放行自动播放（有声）与 Widevine DRM（EME），其余权限走默认。
            session.permissionDelegate = object : GeckoSession.PermissionDelegate {
                override fun onContentPermissionRequest(
                    s: GeckoSession,
                    perm: GeckoSession.PermissionDelegate.ContentPermission,
                ): GeckoResult<Int>? = when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE,
                    GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS ->
                        GeckoResult.fromValue(
                            GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW,
                        )
                    else -> null
                }
            }

            session.open(MyTvApp.geckoRuntime(context))
            view.setSession(session)
            session.loadUri(channel.url)

            holder.session = session
            holder.loadedUrl = channel.url

            onBackHandlerReady { holder.session?.goBack() }

            view
        },
        update = {
            val session = holder.session ?: return@AndroidView
            if (holder.loadedUrl == null || !sameGeckoUrl(holder.loadedUrl, channel.url)) {
                session.loadUri(channel.url)
                holder.loadedUrl = channel.url
            }
        },
    )
}

/** 持有当前 session 与已加载 URL。 */
private class GeckoHolder {
    var session: GeckoSession? = null
    var loadedUrl: String? = null
}

private fun sameGeckoUrl(current: String?, target: String): Boolean {
    if (current == null) return false
    return current.startsWith(target) || target.startsWith(current)
}
