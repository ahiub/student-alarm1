package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.AppThemeEnum
import java.time.LocalTime

data class ThemeColors(
    val colorScheme: ColorScheme,
    val backgroundBrush: Brush,
    val cardBackground: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val isGlass: Boolean = false,
    val isLight: Boolean = false
)

object ThemeEngine {

    fun getThemeColors(theme: AppThemeEnum, isSystemInDarkTheme: Boolean = true): ThemeColors {
        val resolvedTheme = if (theme == AppThemeEnum.AUTO) {
            val hour = LocalTime.now().hour
            val isDay = hour in 6..18
            if (!isSystemInDarkTheme || isDay) AppThemeEnum.STUDY_DESK_LIGHT else AppThemeEnum.NEON_GLOW
        } else {
            theme
        }

        return when (resolvedTheme) {
            AppThemeEnum.NEON_GLOW -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = BrandBlue, // #2EC8FF
                    onPrimary = Color(0xFF03142A),
                    primaryContainer = Color(0xFF0B2D4F),
                    onPrimaryContainer = Color(0xFFBAE6FD),
                    secondary = BrandPurple, // #8B5CF6
                    onSecondary = Color(0xFF1E0A44),
                    secondaryContainer = Color(0xFF381B6E),
                    onSecondaryContainer = Color(0xFFE9D5FF),
                    tertiary = BrandOrange, // #FF9F43
                    onTertiary = Color(0xFF3B1A00),
                    tertiaryContainer = Color(0xFF6B3300),
                    onTertiaryContainer = Color(0xFFFFD8B2),
                    background = Color(0xFF070B1F),
                    onBackground = Color(0xFFF8FAFC),
                    surface = Color(0xFF0F1738),
                    onSurface = Color(0xFFF8FAFC),
                    surfaceVariant = Color(0xFF17224D),
                    onSurfaceVariant = Color(0xFF94A3B8),
                    outline = Color(0xFF28366E),
                    error = Color(0xFFEF4444),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF070B1F), Color(0xFF0C1233))),
                cardBackground = Color(0xFF0F1738),
                cardBorder = Color(0xFF28366E),
                primaryAccent = BrandBlue,
                secondaryAccent = BrandPurple,
                tertiaryAccent = BrandOrange,
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.GLASS -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color(0xFF002A3D),
                    primaryContainer = Color(0xFF034A6E),
                    onPrimaryContainer = Color(0xFFBAE6FD),
                    secondary = Color(0xFFA855F7),
                    onSecondary = Color(0xFF2E054D),
                    secondaryContainer = Color(0xFF4A1078),
                    onSecondaryContainer = Color(0xFFE9D5FF),
                    tertiary = Color(0xFFFB923C),
                    onTertiary = Color(0xFF3D1600),
                    tertiaryContainer = Color(0xFF6A2A00),
                    onTertiaryContainer = Color(0xFFFFDBC2),
                    background = Color(0xFF0B1028),
                    onBackground = Color(0xFFFFFFFF),
                    surface = Color(0x33FFFFFF), // Translucent frosted glass
                    onSurface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0x22FFFFFF),
                    onSurfaceVariant = Color(0xFFCBD5E1),
                    outline = Color(0x55FFFFFF),
                    error = Color(0xFFF87171),
                    onError = Color.White
                ),
                backgroundBrush = Brush.linearGradient(
                    listOf(Color(0xFF140D30), Color(0xFF0B1432), Color(0xFF1C0D2C))
                ),
                cardBackground = Color(0x33FFFFFF),
                cardBorder = Color(0x55FFFFFF),
                primaryAccent = Color(0xFF38BDF8),
                secondaryAccent = Color(0xFFA855F7),
                tertiaryAccent = Color(0xFFFB923C),
                isGlass = true,
                isLight = false
            )

            AppThemeEnum.MIDNIGHT_DARK -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF1E293B),
                    onPrimaryContainer = Color(0xFFE2E8F0),
                    secondary = Color(0xFF818CF8),
                    onSecondary = Color.Black,
                    secondaryContainer = Color(0xFF312E81),
                    onSecondaryContainer = Color(0xFFE0E7FF),
                    tertiary = Color(0xFF34D399),
                    onTertiary = Color.Black,
                    tertiaryContainer = Color(0xFF064E3B),
                    onTertiaryContainer = Color(0xFFA7F3D0),
                    background = Color(0xFF000000),
                    onBackground = Color(0xFFF1F5F9),
                    surface = Color(0xFF0E0E0E),
                    onSurface = Color(0xFFF1F5F9),
                    surfaceVariant = Color(0xFF1C1C1C),
                    onSurfaceVariant = Color(0xFFA1A1AA),
                    outline = Color(0xFF2E2E2E),
                    error = Color(0xFFEF4444),
                    onError = Color.Black
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF080808))),
                cardBackground = Color(0xFF0E0E0E),
                cardBorder = Color(0xFF2E2E2E),
                primaryAccent = Color(0xFF38BDF8),
                secondaryAccent = Color(0xFF818CF8),
                tertiaryAccent = Color(0xFF34D399),
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.STUDY_DESK_LIGHT -> ThemeColors(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFB45309), // Warm amber
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFEF3C7),
                    onPrimaryContainer = Color(0xFF78350F),
                    secondary = Color(0xFF78350F),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFDE68A),
                    onSecondaryContainer = Color(0xFF451A03),
                    tertiary = Color(0xFF059669),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFA7F3D0),
                    onTertiaryContainer = Color(0xFF064E3B),
                    background = Color(0xFFFBF8EE), // Cream paper
                    onBackground = Color(0xFF451A03),
                    surface = Color(0xFFFFFDF5),
                    onSurface = Color(0xFF451A03),
                    surfaceVariant = Color(0xFFF4EEDB),
                    onSurfaceVariant = Color(0xFF78350F),
                    outline = Color(0xFFE5DBC7),
                    error = Color(0xFFDC2626),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFBF8EE), Color(0xFFF3EAD3))),
                cardBackground = Color(0xFFFFFDF5),
                cardBorder = Color(0xFFE5DBC7),
                primaryAccent = Color(0xFFB45309),
                secondaryAccent = Color(0xFF78350F),
                tertiaryAccent = Color(0xFF059669),
                isGlass = false,
                isLight = true
            )

            AppThemeEnum.SUNRISE -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF9F43),
                    onPrimary = Color(0xFF3B1A00),
                    primaryContainer = Color(0xFF6B3300),
                    onPrimaryContainer = Color(0xFFFFD8B2),
                    secondary = Color(0xFFFF6B6B),
                    onSecondary = Color(0xFF400A0A),
                    secondaryContainer = Color(0xFF7A1515),
                    onSecondaryContainer = Color(0xFFFFD5D5),
                    tertiary = Color(0xFFFFC93C),
                    onTertiary = Color(0xFF3B2E00),
                    tertiaryContainer = Color(0xFF6E5600),
                    onTertiaryContainer = Color(0xFFFFEFA6),
                    background = Color(0xFF1D0B14),
                    onBackground = Color(0xFFFFF5EA),
                    surface = Color(0xFF2B121F),
                    onSurface = Color(0xFFFFF5EA),
                    surfaceVariant = Color(0xFF3F192E),
                    onSurfaceVariant = Color(0xFFE2B79A),
                    outline = Color(0xFF5A2544),
                    error = Color(0xFFFF4D4D),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF260D1B), Color(0xFF160610))),
                cardBackground = Color(0xFF2B121F),
                cardBorder = Color(0xFF5A2544),
                primaryAccent = Color(0xFFFF9F43),
                secondaryAccent = Color(0xFFFF6B6B),
                tertiaryAccent = Color(0xFFFFC93C),
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.OCEAN -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    onPrimary = Color(0xFF00373E),
                    primaryContainer = Color(0xFF00535E),
                    onPrimaryContainer = Color(0xFFB5F4FF),
                    secondary = Color(0xFF00B4D8),
                    onSecondary = Color(0xFF003541),
                    secondaryContainer = Color(0xFF00566A),
                    onSecondaryContainer = Color(0xFFB8EFFF),
                    tertiary = Color(0xFF48CAE4),
                    onTertiary = Color(0xFF003640),
                    tertiaryContainer = Color(0xFF005869),
                    onTertiaryContainer = Color(0xFFD4F6FF),
                    background = Color(0xFF061325),
                    onBackground = Color(0xFFF0F9FF),
                    surface = Color(0xFF0C213D),
                    onSurface = Color(0xFFF0F9FF),
                    surfaceVariant = Color(0xFF132F54),
                    onSurfaceVariant = Color(0xFFBAE6FD),
                    outline = Color(0xFF1E4880),
                    error = Color(0xFFFF5252),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF061325), Color(0xFF091C36))),
                cardBackground = Color(0xFF0C213D),
                cardBorder = Color(0xFF1E4880),
                primaryAccent = Color(0xFF00E5FF),
                secondaryAccent = Color(0xFF00B4D8),
                tertiaryAccent = Color(0xFF48CAE4),
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.FOREST -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF10B981),
                    onPrimary = Color(0xFF003824),
                    primaryContainer = Color(0xFF005537),
                    onPrimaryContainer = Color(0xFFA7F3D0),
                    secondary = Color(0xFF84CC16),
                    onSecondary = Color(0xFF243B00),
                    secondaryContainer = Color(0xFF3E6300),
                    onSecondaryContainer = Color(0xFFD9F99D),
                    tertiary = Color(0xFF34D399),
                    onTertiary = Color(0xFF003823),
                    tertiaryContainer = Color(0xFF005536),
                    onTertiaryContainer = Color(0xFFA7F3D0),
                    background = Color(0xFF07170E),
                    onBackground = Color(0xFFF0FDF4),
                    surface = Color(0xFF0E2B1B),
                    onSurface = Color(0xFFF0FDF4),
                    surfaceVariant = Color(0xFF163E27),
                    onSurfaceVariant = Color(0xFFBBF7D0),
                    outline = Color(0xFF235C3C),
                    error = Color(0xFFEF4444),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF07170E), Color(0xFF0B2114))),
                cardBackground = Color(0xFF0E2B1B),
                cardBorder = Color(0xFF235C3C),
                primaryAccent = Color(0xFF10B981),
                secondaryAccent = Color(0xFF84CC16),
                tertiaryAccent = Color(0xFF34D399),
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.ROSE_SUNSET -> ThemeColors(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF43F5E),
                    onPrimary = Color(0xFF4C0015),
                    primaryContainer = Color(0xFF750022),
                    onPrimaryContainer = Color(0xFFFFD9DF),
                    secondary = Color(0xFFC084FC),
                    onSecondary = Color(0xFF380064),
                    secondaryContainer = Color(0xFF5A1494),
                    onSecondaryContainer = Color(0xFFF1D8FF),
                    tertiary = Color(0xFFFB7185),
                    onTertiary = Color(0xFF4D0017),
                    tertiaryContainer = Color(0xFF7A0426),
                    onTertiaryContainer = Color(0xFFFFD9E0),
                    background = Color(0xFF17091A),
                    onBackground = Color(0xFFFFF1F2),
                    surface = Color(0xFF28112E),
                    onSurface = Color(0xFFFFF1F2),
                    surfaceVariant = Color(0xFF3E1B46),
                    onSurfaceVariant = Color(0xFFFECDD3),
                    outline = Color(0xFF5E276B),
                    error = Color(0xFFFF4961),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF1D0B20), Color(0xFF120514))),
                cardBackground = Color(0xFF28112E),
                cardBorder = Color(0xFF5E276B),
                primaryAccent = Color(0xFFF43F5E),
                secondaryAccent = Color(0xFFC084FC),
                tertiaryAccent = Color(0xFFFB7185),
                isGlass = false,
                isLight = false
            )

            AppThemeEnum.MINIMAL_WHITE -> ThemeColors(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0F172A),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE2E8F0),
                    onPrimaryContainer = Color(0xFF0F172A),
                    secondary = Color(0xFF334155),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFF1F5F9),
                    onSecondaryContainer = Color(0xFF1E293B),
                    tertiary = Color(0xFF2563EB),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFDBEAFE),
                    onTertiaryContainer = Color(0xFF1E40AF),
                    background = Color(0xFFFFFFFF),
                    onBackground = Color(0xFF0F172A),
                    surface = Color(0xFFF8FAFC),
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = Color(0xFFF1F5F9),
                    onSurfaceVariant = Color(0xFF475569),
                    outline = Color(0xFFCBD5E1),
                    error = Color(0xFFDC2626),
                    onError = Color.White
                ),
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))),
                cardBackground = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE2E8F0),
                primaryAccent = Color(0xFF0F172A),
                secondaryAccent = Color(0xFF334155),
                tertiaryAccent = Color(0xFF2563EB),
                isGlass = false,
                isLight = true
            )

            AppThemeEnum.AUTO -> {
                // Should not reach here due to resolvedTheme, but fallback safely
                getThemeColors(AppThemeEnum.NEON_GLOW, isSystemInDarkTheme)
            }
        }
    }
}
