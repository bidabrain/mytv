package com.mytv.live.ui.player

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mytv.live.MainActivity
import com.mytv.live.data.Channels
import com.mytv.live.web.WebPlayer
import kotlinx.coroutines.delay

/**
 * 播放页 —— 套壳网页全屏播放。
 *
 * - 进入即横屏 + 沉浸式（隐藏状态/导航栏）+ 常亮，退出还原；
 * - 遥控器上/下（或频道+/-）切台，在同一个 WebView 内重载，省去重建；
 * - 返回键：先退网页全屏/后退，否则回到首页；
 * - 切台时短暂浮出频道名提示。
 */
@Composable
fun PlayerScreen(startChannelId: Int, onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var currentId by remember { mutableIntStateOf(startChannelId) }
    val channel = Channels.byId(currentId) ?: Channels.all.first()

    var showInfo by remember { mutableStateOf(true) }
    var webBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // 切台/进入时浮出频道名 2 秒
    LaunchedEffect(currentId) {
        showInfo = true
        delay(2000)
        showInfo = false
    }

    // 横屏 + 沉浸式 + 常亮，离开时还原
    DisposableEffect(Unit) {
        val window = activity?.window
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 遥控器切台：上一台/下一台（DPAD 或频道键），交给 Activity 的 dispatchKeyEvent 分发
    DisposableEffect(activity) {
        activity?.keyHandler = handler@{ event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@handler false
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    currentId = Channels.prev(currentId).id; true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    currentId = Channels.next(currentId).id; true
                }
                else -> false
            }
        }
        onDispose { activity?.keyHandler = null }
    }

    // 返回键：网页能后退则先后退，否则回首页
    BackHandler {
        if (canGoBack) webBack?.invoke() else onExit()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        WebPlayer(
            channel = channel,
            modifier = Modifier.fillMaxSize(),
            onCanGoBackChanged = { canGoBack = it },
            onBackHandlerReady = { webBack = it },
        )

        AnimatedVisibility(
            visible = showInfo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
        ) {
            Box(
                Modifier
                    .background(
                        Color(0xCC000000),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${currentId + 1}  ${channel.name}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
