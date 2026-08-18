package com.tdull.webdavviewer.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 更多操作菜单的公共浮层容器。
 *
 * 样式与文件列表 menu_redesign 设计稿一致：白底 16dp 圆角容器 + 6dp 阴影 + 168dp 宽，
 * 锚定到触发按钮的 TopEnd；展开时带"从小变大 + 淡入"动效（对标 Material3 DropdownMenu 的展开观感）。
 *
 * @param expanded 是否展开（false 时不渲染弹层）
 * @param onDismiss 关闭回调（点外部/系统返回时触发）
 * @param content 菜单项内容，由调用方用 [MenuItemRow] 或其它组件组织
 */
@Composable
fun MenuPopupContainer(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (expanded) {
        // 菜单每次打开都是一次全新组合（expanded=false 时整体销毁）。
        // 用 Animatable 从初始值（scale 0.8 / alpha 0）动画到最终值，确保首次出现也播放"从小变大 + 淡入"。
        val scaleAnim = remember { Animatable(0.8f) }
        val alphaAnim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            scaleAnim.animateTo(1f, animationSpec = tween(durationMillis = 120))
            alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 120))
        }

        Popup(
            onDismissRequest = onDismiss,
            alignment = Alignment.TopEnd,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(1f, 0f)
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
            ) {
                Column(modifier = Modifier.width(168.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * 更多操作菜单项行（图标 + 文字，44dp 高，点击带涟漪反馈）。
 *
 * 样式与文件列表 MenuItemRow 一致：16dp 图标 + 12dp 间距 + 14sp 文字。
 */
@Composable
fun MenuItemRow(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
