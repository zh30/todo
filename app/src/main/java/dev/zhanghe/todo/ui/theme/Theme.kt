package dev.zhanghe.todo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DeepDarkGreen,
    background = DeepDarkGreen,
    onBackground = TextWhite,
    surface = SurfaceGreen,
    onSurface = TextWhite
)

@Composable
fun TodoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
