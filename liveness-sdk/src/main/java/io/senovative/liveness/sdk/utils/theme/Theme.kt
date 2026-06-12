package io.senovative.liveness.sdk.utils.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = PureWhite,
    secondary = PureWhite,
    onSecondary = TextDark,
    tertiary = OceanBlueLight,
    onTertiary = TextDark,
    background = Color(0xFFF8F9FA),
    surface = PureWhite,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun MyExploreTheme(
    // Ignore system dark mode to force Light Mode as requested
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}