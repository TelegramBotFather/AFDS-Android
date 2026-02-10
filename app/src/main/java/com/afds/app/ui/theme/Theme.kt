package com.afds.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Website colors: #667eea (blue-purple), #764ba2 (purple), #f093fb (pink)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF667EEA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D4E9E),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFF093FB),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF764BA2),
    onSecondaryContainer = Color(0xFFF5D9FF),
    tertiary = Color(0xFFFFBE0B),
    onTertiary = Color.Black,
    background = Color(0xFF121225),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF121225),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF1E1E38),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFF5576C),
    onError = Color.White,
    outline = Color(0xFF938F99),
    surfaceContainerHighest = Color(0xFF2A2A45),
    surfaceContainerHigh = Color(0xFF222240),
    surfaceContainer = Color(0xFF1A1A35),
    surfaceContainerLow = Color(0xFF16162D),
    surfaceContainerLowest = Color(0xFF0F0F22),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF667EEA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF764BA2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5D9FF),
    onSecondaryContainer = Color(0xFF3D1059),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E0F0),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFF5576C),
    onError = Color.White,
)

@Composable
fun AFDSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}