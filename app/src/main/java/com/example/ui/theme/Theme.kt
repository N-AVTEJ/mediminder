package com.example.ui.theme

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
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = BlueContainer,
    background = SoftBackground,
    surface = CardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = MissedRed,
    errorContainer = MissedRedSurface,
    onError = Color.White,
    onErrorContainer = OnMissedRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFE0F2F1),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF121415),
    surface = Color(0xFF1E2022),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6)
)

@Composable
fun MedicineReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our carefully tuned soft palette for consistency
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
        typography = MedicineTypography,
        content = content
    )
}
