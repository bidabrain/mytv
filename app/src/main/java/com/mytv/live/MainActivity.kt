package com.mytv.live

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mytv.live.ui.home.HomeScreen
import com.mytv.live.ui.player.PlayerScreen
import com.mytv.live.ui.theme.MyTvLiveTheme

class MainActivity : ComponentActivity() {

    /**
     * 播放页注册的遥控器按键处理器。返回 true 表示已消费（用于上/下切台等），
     * 否则交还系统/WebView 默认处理。
     */
    var keyHandler: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyHandler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyTvLiveTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNav()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(onChannelClick = { channel ->
                nav.navigate("player/${channel.id}")
            })
        }
        composable(
            route = "player/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("channelId") ?: 0
            PlayerScreen(
                startChannelId = id,
                onExit = { nav.popBackStack() },
            )
        }
    }
}
