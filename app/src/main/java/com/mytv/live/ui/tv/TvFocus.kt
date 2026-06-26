package com.mytv.live.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 遥控器（D-pad）友好的可点击修饰符。
 *
 * 与 [Modifier.clickable] 一样可点击、可获焦，但额外在获得焦点时绘制一圈高亮边框
 * 与半透明底色，让盒子用户能清楚看到"当前选中"的是哪个控件。
 *
 * 用法：把原来的 `.clickable(onClick = ...)` 直接替换为
 * `.tvClickable(shape = ...) { ... }`，shape 传入与背景一致的形状即可让边框对齐。
 */
@Composable
fun Modifier.tvClickable(
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val highlight = MaterialTheme.colorScheme.primary
    val borderColor by animateColorAsState(
        targetValue = if (focused) highlight else Color.Transparent,
        label = "tvFocusBorder"
    )
    val overlay by animateColorAsState(
        targetValue = if (focused) highlight.copy(alpha = 0.16f) else Color.Transparent,
        label = "tvFocusOverlay"
    )
    return this
        .onFocusChanged { focused = it.isFocused }
        .border(width = if (focused) 3.dp else 0.dp, color = borderColor, shape = shape)
        .background(color = overlay, shape = shape)
        .clickable(enabled = enabled, onClick = onClick)
}
